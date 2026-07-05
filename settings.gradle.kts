pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Notes"

// App host (thin) + módulos. O :core concentra tema/componentes (e domínio/dados
// quando houver); cada tela é um módulo de feature (Android Library) próprio, para
// builds incrementais/paralelos e manutenção isolada. Features dependem só de
// :core:* (nunca de :app nem umas das outras) — a navegação entre elas vive no :app.
include(":app")
include(":core:ui")
include(":core:data")
include(":feature:notepad")
include(":feature:blocos")
include(":feature:settings")
