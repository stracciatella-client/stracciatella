package net.stracciatella.module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonObject;
import net.stracciatella.module.dependency.MavenModuleDependency;
import net.stracciatella.module.dependency.MavenRepository;
import net.stracciatella.module.dependency.ModuleDependency;
import net.stracciatella.module.dependency.ModuleModuleDependency;
import net.stracciatella.module.dependency.Repository;
import net.stracciatella.util.GsonProvider;

public record ModuleConfiguration(String name, String id, String main, Set<String> mixins, Set<String> accessWideners, Set<ModuleDependency> dependencies) {

    private static final String TYPE_DEFAULT = "default";
    private static final String TYPE_MODULE = "module";

    public static ModuleConfiguration parse(JsonObject json) {
        var mixins = new HashSet<String>();
        if (json.has("mixins")) {
            for (var element : json.get("mixins").getAsJsonArray()) {
                mixins.add(element.getAsString());
            }
        }
        var accessWideners = new HashSet<String>();
        if (json.has("accessWideners")) {
            for (var element : json.get("accessWideners").getAsJsonArray()) {
                accessWideners.add(element.getAsString());
            }
        }
        var repositories = new HashMap<String, Repository>();
        repositories.put("central", MavenRepository.CENTRAL);

        if (json.has("repositories")) {
            var mavenRepositories = json.get("repositories").getAsJsonObject();
            for (var key : mavenRepositories.keySet()) {
                var repositoryJson = mavenRepositories.get(key).getAsJsonObject();
                var url = repositoryJson.get("url").getAsString();
                var name = repositoryJson.get("name").getAsString();
                repositories.put(name, new MavenRepository(url));
            }
        }
        var dependencies = new HashSet<ModuleDependency>();
        if (json.has("dependencies")) {
            var dependenciesJson = json.get("dependencies").getAsJsonObject();
            for (var dependencyKey : dependenciesJson.keySet()) {
                var element = dependenciesJson.get(dependencyKey);
                var o = element.getAsJsonObject();
                var type = read(o, "type", TYPE_DEFAULT);
                var group = read(o, "group", null);
                var name = read(o, "name", null);
                var version = read(o, "version", null);
                var repositoryName = read(o, "repository", null);
                if (name == null) throw new IllegalStateException("No name defined for dependency");
                var repository = repositoryName == null ? null : repositories.get(repositoryName);
                if (repository == null && repositoryName != null) throw new IllegalStateException("No repository defined for name " + repositoryName);
                var dependency = switch (type) {
                    case TYPE_DEFAULT -> {
                        if (repository == null) throw new IllegalStateException("No repository defined for dependency " + name);
                        if (group == null) throw new IllegalStateException("No group defined for dependency " + name);
                        if (version == null) throw new IllegalStateException("No version defined for dependency " + name);
                        yield new MavenModuleDependency(repository, group, name, version);
                    }
                    case TYPE_MODULE -> new ModuleModuleDependency(name);
                    default -> throw new IllegalStateException("Unknown module type: " + type);
                };
                dependencies.add(dependency);
            }
        }
        var name = json.get("name").getAsString();
        var id = json.get("id").getAsString();
        var main = json.get("main").getAsString();
        return new ModuleConfiguration(name, id, main, mixins, accessWideners, dependencies);
    }

    @Override
    public String toString() {
        return GsonProvider.gson().toJson(this);
    }

    private static String read(JsonObject json, String key, String defaultValue) {
        if (!json.has(key)) return defaultValue;
        var element = json.get(key);
        if (element.isJsonNull()) return defaultValue;
        return element.getAsString();
    }
}
