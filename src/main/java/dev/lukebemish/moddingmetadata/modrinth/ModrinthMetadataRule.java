package dev.lukebemish.moddingmetadata.modrinth;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.lukebemish.moddingmetadata.Identifier;
import org.gradle.api.artifacts.CacheableRule;
import org.gradle.api.artifacts.ComponentMetadataContext;
import org.gradle.api.artifacts.ComponentMetadataRule;
import org.gradle.api.artifacts.repositories.RepositoryResourceAccessor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import javax.inject.Inject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@CacheableRule
@ApiStatus.Internal
public abstract class ModrinthMetadataRule implements ComponentMetadataRule {
    private static final String USER_AGENT = "lukebemish/ModrinthGradleMetadata/"+ModrinthMetadataRule.class.getPackage().getImplementationVersion();
    private static final String MODRINTH_API = "https://api.modrinth.com/v2";
    private static final Gson GSON = new Gson();
    private static final long[] nextResponseTime = {0};
    private static final HttpClient client = HttpClient.newHttpClient();

    private final Map<String, Identifier> fabricModuleMaps;

    @Inject
    public ModrinthMetadataRule(Map<String, Identifier> fabricModuleMaps) {
        this.fabricModuleMaps = fabricModuleMaps;
    }

    @Contract("_, _, false -> !null")
    private @Nullable JsonElement request(String path, JsonObject json, boolean allow404) {
        var rateLimitWait = Math.min(nextResponseTime[0] - System.currentTimeMillis(), 60_000);
        if (rateLimitWait > 0) {
            try {
                System.out.println("Rate limited by modrinth, waiting " + rateLimitWait + "ms");
                Thread.sleep(nextResponseTime[0] - System.currentTimeMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        StringBuilder pathBuilder = new StringBuilder(path);
        boolean first = true;
        for (var entry : json.entrySet()) {
            pathBuilder.append(first ? "?" : "&")
                    .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(GSON.toJson(entry.getValue()), StandardCharsets.UTF_8));
            first = false;
        }
        var request = HttpRequest.newBuilder(URI.create(MODRINTH_API + pathBuilder))
                .setHeader("User-Agent", USER_AGENT)
                .setHeader("Content-Type", "application/json")
                .GET();

        return client.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (allow404 && response.statusCode() == 404) {
                        return null;
                    }
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Failed to fetch " + path + ", received code " + response.statusCode() + ": " + response.body());
                    }
                    response.headers().firstValue("X-Ratelimit-Remaining").map(Integer::parseInt).ifPresent(remaining -> {
                        if (remaining > 0) {
                            return;
                        }
                        response.headers().firstValue("X-Ratelimit-Reset").map(Integer::parseInt).ifPresent(reset -> {
                            nextResponseTime[0] = System.currentTimeMillis() + (reset+1) * 1000L;
                        });
                    });
                    return response;
                })
                .thenApply(response -> {
                    if (response == null) {
                        return null;
                    }
                    try {
                        return GSON.fromJson(response.body(), JsonElement.class);
                    } catch (RuntimeException e) {
                        throw new RuntimeException("Failed to parse " + path + ": " + response.body(), e);
                    }
                })
                .join();
    }

    @Override
    public void execute(ComponentMetadataContext context) {
        if (!"maven.modrinth".equals(context.getDetails().getId().getGroup())) {
            return;
        }

        var project = context.getDetails().getId().getName();
        var version = context.getDetails().getId().getVersion();

        var path = "/project/"+project+"/version/"+version;
        var result = request(path, new JsonObject(), false).getAsJsonObject();
        var projectId = result.get("project_id").getAsString();
        var projectSlug = request("/project/"+projectId, new JsonObject(), false).getAsJsonObject().get("slug").getAsString();

        context.getDetails().allVariants(variant -> {
            variant.withCapabilities(capabilities -> {
                capabilities.addCapability("maven.modrinth", projectId, version);
                capabilities.addCapability("maven.modrinth", projectSlug, version);
            });
        });

        Instant datePublished = OffsetDateTime.parse(result.getAsJsonObject().get("date_published").getAsString()).toInstant();
        var loaders = new HashSet<String>();
        result.getAsJsonObject().get("loaders").getAsJsonArray().forEach(loader -> {
            loaders.add(loader.getAsString());
        });

        result.get("dependencies").getAsJsonArray().forEach(dependency -> {
            if (!"required".equals(dependency.getAsJsonObject().get("dependency_type").getAsString()) && !"embedded".equals(dependency.getAsJsonObject().get("dependency_type").getAsString())) {
                return;
            }
            var depVersion = dependency.getAsJsonObject().get("version_id");
            var depProject = dependency.getAsJsonObject().get("project_id").getAsString();

            context.getDetails().withVariant("runtime", variant -> {
                variant.withDependencies(dependencies -> {
                    var map = new HashMap<String, String>();
                    map.put("group", "maven.modrinth");
                    map.put("name", depProject);
                    String finalDepVersion;
                    if (depVersion != null && !depVersion.isJsonNull()) {
                        finalDepVersion = depVersion.getAsString();
                    } else {
                        var depProjectPath = "/project/" + depProject + "/version";
                        var parameters = new JsonObject();
                        parameters.add("game_versions", result.getAsJsonObject().get("game_versions"));
                        String foundVersion = null;
                        Instant foundDate = null;
                        for (var versionElement : request(depProjectPath, parameters, false).getAsJsonArray()) {
                            boolean validLoader = false;
                            for (var loader : versionElement.getAsJsonObject().get("loaders").getAsJsonArray()) {
                                if (loaders.contains(loader.getAsString())) {
                                    validLoader = true;
                                    break;
                                }
                            }
                            if (!validLoader) {
                                continue;
                            }
                            Instant date = OffsetDateTime.parse(versionElement.getAsJsonObject().get("date_published").getAsString()).toInstant();
                            if (date.compareTo(datePublished) > 0) {
                                continue;
                            }
                            if (foundDate != null && date.compareTo(foundDate) < 0) {
                                continue;
                            }
                            foundDate = date;
                            foundVersion = versionElement.getAsJsonObject().get("id").getAsString();
                        }
                        finalDepVersion = foundVersion;
                    }
                    if (fabricModuleMaps.containsKey(depProject) && finalDepVersion != null) {
                        boolean[] found = {false};
                        fabricModuleMaps.get(depProject).fabricRecover(getRepositoryAccessor(), identifier -> {
                            dependencies.add(Map.of(
                                "group", identifier.identifier().group(),
                                "name", identifier.identifier().name(),
                                "version", identifier.version()
                            ));
                            found[0] = true;
                        }, "maven.modrinth", depProject, finalDepVersion);
                        if (!found[0]) {
                            dependencies.add(map, dep -> dep.version(v -> {
                                v.prefer(finalDepVersion);
                            }));
                        }
                    } else {
                        dependencies.add(map, dep -> dep.version(v -> {
                            if (finalDepVersion != null) {
                                v.prefer(finalDepVersion);
                            }
                        }));
                    }
                });
            });
        });
    }

    @Inject
    protected abstract RepositoryResourceAccessor getRepositoryAccessor();
}
