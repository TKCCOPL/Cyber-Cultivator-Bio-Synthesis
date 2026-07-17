# Repository Guidelines

## Project Structure & Module Organization

This is a Minecraft Forge 1.20.1 mod built with Java 17. Production code lives under `src/main/java/com/TKCCOPL/`. Keep registry declarations in `init/`; gameplay blocks and state machines in `block/` and `block/entity/`; integrations in `compat/` and `curios/`; recipes in `recipe/`; and generated-data providers in `datagen/`.

Hand-authored assets, configuration, recipes, and metadata belong in `src/main/resources/`. Datagen output belongs in `src/generated/resources/`; do not hand-edit generated blockstates, models, loot tables, or `zh_cn.json`. Design notes and test records live in `docs/`, while `simulations/` contains Python balancing experiments. `run/`, `run-data/`, and `build/` are local outputs.

## Build, Test, and Development Commands

- `./gradlew compileJava` — perform a fast Java compilation check.
- `./gradlew build` — compile, process resources, test, and create the reobfuscated JAR in `build/libs/`.
- `./gradlew runData` — regenerate resources after changing a provider in `datagen/`.
- `./gradlew runClient` — launch a Forge client for interactive smoke testing.
- `./gradlew runServer` — launch the headless development server.
- `./gradlew runGameTestServer` — run registered Forge GameTests.

Use `gradlew.bat` for the equivalent commands on Windows.

## Coding Style & Naming Conventions

Use four-space indentation, UTF-8, braces on the same line, and standard Java naming: `PascalCase` types, `camelCase` methods/fields, and `UPPER_SNAKE_CASE` constants. Preserve the existing `com.TKCCOPL` package spelling. Prefer focused classes, `DeferredRegister`/`RegistryObject` registration, and defensive `ItemStack.copy()` calls across API or inventory boundaries. Resource identifiers and JSON filenames must be lowercase `snake_case`, under the `cybercultivator` namespace. No formatter or linter is configured, so match nearby code and remove unused imports.

## Testing Guidelines

`src/test/` is currently empty and no coverage threshold is configured. At minimum, run `compileJava` and `build`; run `runData` for generated-resource changes. For gameplay work, use `runClient` and verify world loading, registration, relevant placement/interaction, tooltips or HUD behavior, and automation paths. Record substantial manual checks in `docs/test-reports/`. Add Forge GameTests for deterministic mechanics where practical.

## Commit & Pull Request Guidelines

History follows Conventional Commit-style prefixes, often with scopes: `feat(recipe): ...`, `fix(blockentity): ...`, `perf(curios): ...`, `docs: ...`, and `release: vX.Y.Z ...`. Keep subjects imperative, specific, and consistent with the repository's Chinese-language history.

Pull requests should explain behavior changes, list validation commands and manual scenarios, link related issues/design documents, and include screenshots for HUD, texture, or UI changes. Commit regenerated resources with their provider changes, and call out compatibility impacts involving Forge, Curios, or JEI.

For a version release, bump `mod_version` in `gradle.properties` and update both README changelogs. After the PR is merged, CI uses the PR title and body as the annotated tag message and Release notes, so describe user-visible updates and fixes explicitly.
