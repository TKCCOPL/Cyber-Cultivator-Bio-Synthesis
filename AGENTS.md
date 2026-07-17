# Repository Guidelines

## Project Baseline

Cyber-Cultivator: Bio-Synthesis is a Minecraft Forge 1.20.1 mod built with Java 17.

- Mod ID: `cybercultivator`
- Java package: `com.TKCCOPL`
- Forge: `47.4.18`
- Mappings: Parchment `2023.09.03-1.20.1`
- Curios API `5.3.5+`: optional accessory integration
- JEI: compatible

Curios is an optional compile-time integration and development-runtime dependency. Keep compatibility code isolated and guard runtime access so the mod starts without optional dependencies.

## Project Structure

Production code lives under `src/main/java/com/TKCCOPL/`.

- `init/` — registries using `DeferredRegister` and `RegistryObject`
- `block/` and `block/entity/` — gameplay blocks, inventories, and state machines
- `item/` and `effect/` — genetic items, serums, effects, and side effects
- `recipe/` — recipe types, serializers, legacy recipe mapping, and public recipe registries
- `api/` and `event/` — public query API, DTOs, and cancellable Forge events
- `curios/` and `compat/` — optional compatibility layers
- `client/` — tooltips, HUD overlays, and client-only presentation
- `datagen/` — generated language, recipes, models, blockstates, tags, loot, advancements, and structures
- `gametest/` — registered Forge GameTests

Hand-authored resources and metadata belong in `src/main/resources/`. Datagen output belongs in `src/generated/resources/`; never hand-edit generated blockstates, models, loot tables, recipes, structures, or `zh_cn.json`. SNBT GameTest sources belong in `src/main/snbt/` and generate NBT templates under `src/generated/resources/data/<namespace>/structures/`.

Design notes, plans, user documentation, and test records live in `docs/`. Offline balancing experiments live in `simulations/`. Treat `run/`, `run-data/`, and `build/` as local output directories.

## Architecture and Compatibility Rules

- Preserve the existing `com.TKCCOPL` package spelling and the `cybercultivator` namespace.
- Declare registry objects in the appropriate `init/` class; do not introduce ad hoc static registration.
- Keep serum bottling recipes data-driven through `RecipeType<SerumRecipe>` JSON.
- Keep API packages independent of compile-only integrations.
- Copy mutable `ItemStack`, array, and collection values at public API, DTO, event, and inventory boundaries. Expose immutable snapshots where mutation is not intended.
- Public API methods must remain null-safe and must not remove or silently change existing signatures without an explicit compatibility decision.
- Serum Activity is read through the shared boundary rules: valid range `1..15`, default `5` for missing or invalid inputs.
- Block entities must call `setChanged()` and synchronize relevant state with `sendBlockUpdated(..., 2)`. Preserve the non-empty update-tag sentinel where an empty tag would suppress client `load()`.
- Keep client-only code out of dedicated-server execution paths.
- Guard Curios access with the existing compatibility checks. Curios item-to-slot mappings and slot/entity bindings remain data-driven.

## Build and Development Commands

- `./gradlew compileJava` — fast Java compilation check
- `./gradlew build` — compile, process resources, test, and create the reobfuscated JAR in `build/libs/`
- `./gradlew runData` — regenerate resources after datagen changes
- `./gradlew runGameTestServer` — run registered Forge GameTests; the executed count must be greater than zero
- `./gradlew runClient` — launch the client for interactive smoke testing
- `./gradlew runServer` — launch the headless development server
- `./gradlew reobfJar` — reobfuscate the JAR; already included by `build`

Use `gradlew.bat` for equivalent commands on Windows.

## Coding and Resource Conventions

Use four-space indentation, UTF-8, braces on the same line, and standard Java naming: `PascalCase` types, `camelCase` members, and `UPPER_SNAKE_CASE` constants. Prefer focused classes, remove unused imports, and match surrounding code because no formatter or linter is configured.

Resource identifiers and JSON filenames must use lowercase `snake_case` under the `cybercultivator` namespace. Generate Chinese translations through `ModLangProvider`; maintain English translations by hand and keep both key sets identical. Regenerate resources with `runData` after changing a provider and commit the provider and generated output together.

Use the existing texture specification in `docs/texture_generation_spec.md`. Do not replace placeholders or introduce generated art unless that work is explicitly in scope.

## Phase Gate and Testing

Every implementation phase must pass its applicable gate before work proceeds or is merged:

1. Static validation:
   - Run `./gradlew compileJava` for code changes.
   - Run `./gradlew runData` for datagen or generated-resource changes.
   - Confirm datagen produces only expected diffs.
2. Build validation:
   - Run `./gradlew build` for code, resource, dependency, or release changes.
3. Deterministic runtime validation:
   - Add or update Forge GameTests when the mechanic can be reproduced deterministically.
   - Run `./gradlew runGameTestServer` and require at least one executed test and no failures.
4. Interactive runtime validation:
   - For gameplay, UI, HUD, localization, texture, or interaction changes, run `./gradlew runClient` when the environment supports it.
   - Verify main-menu startup, world loading, registration, relevant placement/use, tooltips or HUD, and the changed input-to-output path.
5. Record results:
   - Record substantial checks in `docs/test-reports/`, including pass/fail status, relevant log paths, unverified manual steps, and the fixing commit when available.
   - Fix failures and rerun the same gate before continuing.

A gate passes only when there are no blocking crashes, registry failures, missing critical resources, failed required tests, or non-reproducible changed behavior. Documentation-only edits do not require a Gradle build unless they alter build instructions or generated-resource contracts; they still require link, structure, and `git diff --check` validation.

## Documentation Rules

- Keep `README.md` and `README_EN.md` as concise project entry pages. Do not restore embedded historical changelogs.
- Put detailed gameplay, recipes, machine operation, algorithms, and serum values in `docs/USER_GUIDE.md`.
- Keep both README files structurally aligned and update their displayed version only during an actual version release.
- Synchronize plans, specifications, and historical test-report status when behavior changes, while preserving original historical conclusions with a clear current-status note.
- If documentation and code disagree, verify the implementation first, then update the documentation and record the discrepancy.

## Commits and Pull Requests

Use Conventional Commit-style prefixes consistent with repository history, for example `feat(recipe): ...`, `fix(blockentity): ...`, `perf(curios): ...`, `docs: ...`, and `release: vX.Y.Z ...`. Keep subjects imperative, specific, and consistent with the repository's Chinese-language history.

Normal PRs should explain behavior changes, compatibility impact, and relevant manual evidence. Commit regenerated resources with their provider changes. Include screenshots for visual HUD, texture, or UI work when available.

Do not push implementation or release commits directly to `main`; use a branch and PR. Never merge while required checks are pending or failing.

## Version Release Process

Documentation-only edits do not increase the version and do not trigger a release. Increase the version for code, resource, dependency, or user-visible behavior changes.

For every version release, follow this order:

1. Update `mod_version` in `gradle.properties`.
2. Update the displayed version in both `README.md` and `README_EN.md`; do not add a historical changelog.
3. Check whether `CLAUDE.md`, `AGENTS.md`, `docs/USER_GUIDE.md`, plans, specifications, or test reports need synchronization.
4. Run `./gradlew build`, plus `runData`, GameTests, and runtime smoke tests when applicable.
5. Commit with a release subject such as `release: vX.Y.Z 更新与修复` and a body containing only user-visible updates and fixes.
6. Push a version branch and open a PR. The PR title and body become the annotated tag and GitHub Release notes, so include only user-visible updates and fixes—omit audit process, test process, internal plans, and deferred work.
7. Merge only after build, datagen, the Curios runtime smoke test, and the no-optional-dependencies smoke test pass.

After merge, CI uploads `cybercultivator-X.Y.Z.jar`, creates annotated tag `vX.Y.Z`, and creates the matching GitHub Release with the JAR attached. If that release already exists, CI keeps only the workflow artifact; therefore, every real release must use a new version.
