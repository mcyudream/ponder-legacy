#!/usr/bin/env python3
"""Verify Ponder on official Cleanroom 0.4.0 or 0.4.0-alpha (Linux x64)."""
import argparse
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import urllib.request
from zipfile import ZipFile

PROJECT = Path(__file__).resolve().parent.parent
WORKSPACE = Path(os.environ.get("PONDER_WORKSPACE", str(PROJECT.parent.parent)))
VERSION = os.environ.get("PONDER_CLEANROOM_VERSION", "0.4.0")
INSTALLER_HASHES = {
    "0.4.0": "882435d851a51e14b802e0a9384e4a05cfbb3890fca435c40c494af75e5a166f",
    "0.4.0-alpha": "10d9500c96ee83e3a5e55af67d0aca404b2ad9295ef54656496f730fabc90515",
}
if VERSION not in INSTALLER_HASHES:
    raise ValueError("PONDER_CLEANROOM_VERSION must be 0.4.0 or 0.4.0-alpha")
CACHE = WORKSPACE / ".dev" / f"cleanroom-{VERSION}-runtime"
GRADLE = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
PACK = WORKSPACE / ".dev" / f"cleanroom-{VERSION}-installer.jar"
PACK_SHA256 = INSTALLER_HASHES[VERSION]


def digest(path, algorithm="sha1"):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, algorithm).hexdigest()


def download(url, destination, expected, algorithm="sha1", candidates=()):
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.is_file() and digest(destination, algorithm) == expected:
        return destination
    for candidate in candidates:
        if candidate.is_file() and digest(candidate, algorithm) == expected:
            shutil.copyfile(candidate, destination)
            return destination
    urls = [url]
    if url.startswith("https://repo.cleanroommc.com/releases/"):
        urls.insert(0, url.replace("https://repo.cleanroommc.com/releases/", "https://maven.cleanroommc.com/"))
    last_error = None
    for attempt in range(2):
        for source in urls:
            try:
                request = urllib.request.Request(source, headers={"User-Agent": "Ponder-Cleanroom-verification"})
                partial = destination.with_name(destination.name + f".{os.getpid()}.part")
                with urllib.request.urlopen(request, timeout=30) as response, partial.open("wb") as output:
                    shutil.copyfileobj(response, output, 128 * 1024)
                if digest(partial, algorithm) != expected:
                    raise ValueError(f"Hash mismatch: {destination.name}")
                partial.replace(destination)
                print("Downloaded", destination.name, flush=True)
                return destination
            except (OSError, ValueError) as error:
                last_error = error
    raise RuntimeError(f"Could not download {url}: {last_error}")


def allowed(library):
    rules = library.get("rules", [])
    result = not rules
    for rule in rules:
        os_rule = rule.get("os", {})
        if os_rule.get("name", "linux") == "linux" and os_rule.get("arch", "x86_64") in ("x86_64", "amd64"):
            result = rule["action"] == "allow"
    # The historical MMC recipe also lists ARM natives with a plain Linux rule.
    classifier = library["name"].split(":")[3:]
    return result and (not classifier or not classifier[0].startswith("natives-") or classifier == ["natives-linux"])


def artifact(library):
    entry = library["downloads"]["artifact"]
    parts = library["name"].split(":")
    group, module, version = parts[:3]
    path = entry.get("path") or f"{group.replace('.', '/')}/{module}/{version}/{entry['url'].rsplit('/', 1)[-1]}"
    candidates = (GRADLE / "caches/modules-2/files-2.1" / group / module / version).glob(f"*/{Path(path).name}")
    candidates = list(candidates)
    if module == "cleanroom":
        candidates.append(WORKSPACE / ".dev" / f"cleanroom-{VERSION}-universal.jar")
    # Reuse byte-identical libraries from the old verification cache only after SHA-1 validation.
    candidates += list((WORKSPACE / ".dev/cleanroom-0.4.0-alpha-runtime/libraries").glob("**/" + Path(path).name))
    if module == "cleanroom":
        entry = dict(entry, url=f"https://maven.cleanroommc.com/com/cleanroommc/cleanroom/{VERSION}/cleanroom-{VERSION}-universal.jar")
    return download(entry["url"], CACHE / "libraries" / path, entry["sha1"], candidates=candidates)


def prepare(side):
    download(f"https://maven.cleanroommc.com/com/cleanroommc/cleanroom/{VERSION}/{PACK.name}", PACK, PACK_SHA256, "sha256")
    with ZipFile(PACK) as archive:
        recipe = json.loads(archive.read("version.json"))
    if recipe["id"] != f"cleanroom-{VERSION}":
        raise ValueError("Unexpected Cleanroom version in the official installer")
    metadata = json.loads((GRADLE / "caches/cleanroom_gradle/versions/1.12.2/meta.json").read_text())
    # Cleanroom replaces vanilla's obsolete LWJGL, Netty, ICU, JNA, logging and utility libraries.
    # Retain only the vanilla audio, authentication and platform services used by the official recipe.
    retained = {"com.paulscode", "com.mojang:authlib", "com.mojang:realms", "com.mojang:text2speech", "ca.weblite:java-objc-bridge"}
    vanilla_libraries = [library for library in metadata["libraries"]
                        if library["name"].split(":")[0] in retained or ":".join(library["name"].split(":")[:2]) in retained]
    libraries = {}
    for group in (recipe["libraries"], vanilla_libraries):
        for library in group:
            if allowed(library):
                libraries.setdefault(library["name"], library)
                native = library.get("natives", {}).get("linux")
                if native:
                    libraries.setdefault(library["name"] + ":" + native, {
                        "name": library["name"] + ":" + native,
                        "downloads": {"artifact": library["downloads"]["classifiers"][native]},
                    })
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        classpath = list(pool.map(artifact, libraries.values()))
    main = metadata["downloads"][side]
    vanilla = GRADLE / f"caches/cleanroom_gradle/versions/1.12.2/{side}.jar"
    classpath.append(download(main["url"], CACHE / f"minecraft-{side}-1.12.2.jar", main["sha1"], candidates=[vanilla]))
    # Never let concurrent verifiers overwrite ELF files mapped by this JVM.
    natives = PROJECT / "build/verification-natives" / str(os.getpid())
    natives.mkdir(parents=True, exist_ok=True)
    for jar in classpath:
        if "natives-linux" in jar.name:
            with ZipFile(jar) as archive:
                for name in archive.namelist():
                    if name.endswith(".so"):
                        target = natives / Path(name).name
                        data = archive.read(name)
                        # Another client may already have this library mapped. Never truncate it in place.
                        if not target.is_file() or target.read_bytes() != data:
                            partial = target.with_name(target.name + f".{os.getpid()}.part")
                            partial.write_bytes(data)
                            partial.replace(target)
    return classpath, natives



def main():
    parser = argparse.ArgumentParser(description=f"Verify Ponder installation jars on Cleanroom {VERSION} (Linux x64).")
    parser.add_argument("side", choices=["client", "server"])
    parser.add_argument("--prepare-only", action="store_true")
    parser.add_argument("--empty", action="store_true", help="Attempt loader-only dedicated server startup")
    parser.add_argument("--crafttweaker", choices=["present", "absent"], help="Verify optional CraftTweaker in an isolated production-jar instance")
    parser.add_argument("--mmce", choices=["present", "absent", "incompatible"], help="Verify optional MMCE using isolated production-jar fixtures")
    parser.add_argument("--revo", choices=["present", "absent"], help="Verify tooltip geometry with an isolated Revo UI fixture")
    parser.add_argument("--revo-dir", type=Path, default=WORKSPACE / ".dev/revo-ui", help="Directory containing the official Revo UI 0.8.7, NFR 0.8.0 and ModularUI 3.2.0-nfr.2 jars")
    parser.add_argument("--ae2s", type=Path, help="Optional AE2 Supergiant production jar for the gray smart cable tooltip fixture")
    parser.add_argument("--optional-jar", action="append", type=Path, default=[], help="Additional independently installed optional mod")
    parser.add_argument("--fixture-name", help="Use an isolated named fixture directory")
    parser.add_argument("--vlc-root", type=Path, help="Optional unpacked VLC installation for Linux video verification")
    parser.add_argument("--video-test", type=Path, help="Copy a local test clip to the isolated guide media directory")
    parser.add_argument("--media-editor-only", action="store_true", help="Run only Markdown media authoring screens after client world startup")
    parser.add_argument("--catalog-only", action="store_true", help="Run only catalog category, localization, search and layout checks after client world startup")
    parser.add_argument("--gui-tutorial-only", action="store_true", help="Run native furnace GUI guidance, isolated demonstration and authoring acceptance")
    args = parser.parse_args()
    if platform.system() != "Linux" or platform.machine() not in ("x86_64", "amd64"):
        parser.error("This development launcher supports Linux x64")
    classpath, natives = prepare(args.side)
    if args.prepare_only:
        return
    if args.empty and args.side != "server":
        parser.error("--empty is a dedicated-server probe")
    if sum((args.media_editor_only,args.catalog_only,args.gui_tutorial_only))>1:
        parser.error("Choose one focused client scenario")
    if (args.media_editor_only or args.catalog_only or args.gui_tutorial_only) and (args.side != "client" or args.revo or args.mmce):
        parser.error("Focused client scenarios require the editor client harness; install UI mods with --optional-jar")
    if sum((args.empty, bool(args.mmce), bool(args.revo))) > 1:
        parser.error("--empty, --mmce and --revo are separate fixtures")
    if args.ae2s and not args.revo:
        parser.error("--ae2s requires --revo")
    if args.crafttweaker == "absent" and args.mmce in ("present", "incompatible"):
        parser.error("Official MMCE 2.3.2 requires CraftTweaker itself; this combination cannot start")
    run = PROJECT / "run" / f"cleanroom-{VERSION}-legacy" / ("empty-server" if args.empty else args.side)
    if args.mmce:
        run = PROJECT / "run" / f"cleanroom-{VERSION}-mmce" / args.mmce / args.side
    if args.revo:
        run = PROJECT / "run" / f"cleanroom-{VERSION}-revo" / args.revo / args.side
    if args.crafttweaker:
        fixture_name = "mmce-" + args.mmce if args.mmce else "revo-" + args.revo if args.revo else "core"
        run = PROJECT / "run" / f"cleanroom-{VERSION}-crafttweaker" / args.crafttweaker / fixture_name / args.side
    if args.fixture_name:
        if not re.fullmatch(r"[a-z0-9-]+", args.fixture_name):
            parser.error("Invalid fixture name")
        run = PROJECT / "run" / f"cleanroom-{VERSION}-optional" / args.fixture_name / args.side
    mods = run / "mods"
    mods.mkdir(parents=True, exist_ok=True)
    harness = "ponder-editor-harness-srg.jar" if args.side == "client" else "ponder-server-harness-srg.jar"
    if args.mmce:
        harness = "ponder-mmce-harness-srg.jar"
    if args.revo and args.side == "client":
        harness = "ponder-revo-harness-srg.jar"
    for obsolete in {"ponder-editor-harness-srg.jar", "ponder-server-harness-srg.jar", "ponder-mmce-harness-srg.jar", "ponder-revo-harness-srg.jar"} - {harness}:
        (mods / obsolete).unlink(missing_ok=True)
    version = re.search(r"^version = '([^']+)'", (PROJECT / "build.gradle").read_text(), re.MULTILINE).group(1)
    production = PROJECT / f"build/libs/ponder-legacy-{version}.jar"
    for obsolete in mods.glob("ponder-legacy-*.jar"):
        if obsolete.name != production.name:
            obsolete.unlink()
    jars = [] if args.empty else [production, PROJECT / "build/dev" / harness]
    if not args.empty and args.crafttweaker != "absent":
        crafttweaker = list((GRADLE / "caches/modules-2/files-2.1/maven.modrinth/crafttweaker/4.1.20.698").glob("*/*.jar"))
        if len(crafttweaker) != 1:
            raise RuntimeError("Run tools/dev.sh build to resolve CraftTweaker 4.1.20.698")
        jars.append(crafttweaker[0])
    if args.crafttweaker == "absent":
        for obsolete in mods.glob("crafttweaker-*.jar"):
            obsolete.unlink()
    if args.crafttweaker and not args.mmce and (not args.revo or args.side == "server"):
        # Keep the same .zs file in both fixtures to verify it is inert without CrT.
        script = run / "scripts/ponder/scenes/optional-crafttweaker.zs"
        script.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(PROJECT / "src/dev/fixtures/optional-crafttweaker.zs", script)
    if args.revo == "present":
        jars += [args.revo_dir / name for name in (
            "neofontrender-0.8.0-full.jar", "neofontrender-ui-enhancements-0.8.7.jar", "modularui-3.2.0-nfr.2.jar")]
    if args.ae2s:
        jars.append(args.ae2s.resolve())
    if args.mmce in ("present", "incompatible"):
        mmce = list((GRADLE / "caches/modules-2/files-2.1/curse.maven/modularmachinery-community-edition-817377/7372951").glob("*/*.jar"))
        if len(mmce) != 1:
            raise RuntimeError("Run tools/dev.sh build to resolve MMCE 2.3.2")
        jars.append(mmce[0])
        fixture = PROJECT / "ponder-mmce/examples/smoke"
        shutil.copytree(fixture / "config", run / "config", dirs_exist_ok=True)
        # Examples are installed explicitly in this isolated test instance only.
        if args.mmce == "present":
            shutil.copytree(fixture / "scripts", run / "scripts", dirs_exist_ok=True)
    jars += [jar.resolve() for jar in args.optional_jar]
    if args.video_test:
        media = run / "scripts/ponder/media/guide-smoke.mp4"
        media.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(args.video_test, media)
    for jar in jars:
        if not jar.is_file():
            raise FileNotFoundError(f"Build first: tools/dev.sh build reobfDevHarnessJar; missing {jar}")
        shutil.copyfile(jar, mods / jar.name)
    # The guide fixture is authored during the run and must not affect earlier catalog checks on rerun.
    (run / "scripts/ponder/guides/smoke/markdown_network.md").unlink(missing_ok=True)
    config = run / "config"
    config.mkdir(exist_ok=True)
    (config / "splash.properties").write_text("enabled=false\n")
    if args.revo == "present" and args.side == "client":
        # This directory belongs to the verifier. Do not modify any dependency jar
        # to work around NFR 0.8.0's invalid first-run default TOML.
        (config / "neofontrender.toml").write_text(
            (PROJECT / "src/revoHarness/fixtures/neofontrender.toml").read_text())
    jdk = Path(os.environ.get("PONDER_JAVA_HOME", WORKSPACE / ".dev/jdk-25.0.5+8"))
    command = [str(jdk / "bin/java"), "-Xmx3G", "-Dfile.encoding=UTF-8", f"-Djava.library.path={natives}",
               "-cp", os.pathsep.join(map(str, classpath))]
    if args.vlc_root:
        command += ["-Djna.library.path=" + str(args.vlc_root.resolve() / "usr/lib/x86_64-linux-gnu")]
    if args.mmce or args.revo or args.crafttweaker:
        command += ["-Xlog:class+load=info:file=class-load.log"]
    if args.crafttweaker:
        command += [f"-Dponder.crafttweaker.smoke={args.crafttweaker}"]
    if args.mmce:
        command += [f"-Dponder.mmce.smoke={args.mmce}"]
        if args.mmce == "incompatible":
            command += ["-Dponder.mmce.verification.forceIncompatibleAbi=true"]
    if args.revo:
        command += [f"-Dponder.revo.smoke={args.revo}"]
        if args.ae2s:
            command += ["-Dponder.revo.requireAe2s=true"]
    if args.side == "client":
        report = run / "ponder-smoke/report.json"
        if args.media_editor_only:
            command += ["-Dponder.dev.mediaEditorOnly=true"]
        if args.catalog_only:
            command += ["-Dponder.dev.catalogOnly=true"]
        if args.gui_tutorial_only:
            command += ["-Dponder.dev.guiTutorialOnly=true"]
        command += ["-Dponder.dev.smoke=true", "top.outlands.foundation.boot.Foundation", "--tweakClass", "net.minecraftforge.fml.common.launcher.FMLTweaker",
                    "--username", "PonderDeveloper", "--uuid", "b4551621-016f-3568-8ef0-15ec7d7d579b", "--accessToken", "0", "--version", f"Cleanroom-{VERSION}",
                    "--gameDir", str(run), "--assetsDir", str(GRADLE / "caches/cleanroom_gradle/assets"), "--assetIndex", "1.12", "--width", "1280", "--height", "720"]
    else:
        (run / "eula.txt").write_text("eula=true\n")
        (run / "server.properties").write_text("server-ip=127.0.0.1\nserver-port=25684\nonline-mode=false\nlevel-name=ponder-verification\nlevel-type=FLAT\nview-distance=2\ngenerate-structures=false\n")
        report = run / "ponder-server-smoke.json"
        command += ["-Dponder.dev.serverSmoke=true", "net.minecraftforge.fml.relauncher.ServerLaunchWrapper", "nogui"]
    if args.mmce:
        report = run / "ponder-mmce-report.json"
    if args.revo and args.side == "client":
        report = run / "ponder-revo-smoke/report.json"
    report.unlink(missing_ok=True)
    (run / "runtime.json").write_text(json.dumps({"cleanroom": VERSION, "side": args.side,
        "scenario": "gui-tutorial" if args.gui_tutorial_only else "catalog" if args.catalog_only else "markdown-media-editor" if args.media_editor_only else "full",
        "jars": {jar.name: digest(jar, "sha256") for jar in jars},
        "libraries": {str(jar): digest(jar) for jar in classpath}}, indent=2) + "\n")
    print(f"Starting Cleanroom {VERSION} {args.side}; installation jars and SRG harness, no userdev", flush=True)
    environment = os.environ.copy()
    if args.vlc_root:
        native = args.vlc_root.resolve() / "usr/lib/x86_64-linux-gnu"
        environment["LD_LIBRARY_PATH"] = str(native) + os.pathsep + environment.get("LD_LIBRARY_PATH", "")
        environment["VLC_PLUGIN_PATH"] = str(native / "vlc/plugins")
    try:
        result = subprocess.run(command, cwd=run, env=environment, timeout=45 if args.empty else 300)
    except subprocess.TimeoutExpired:
        raise SystemExit(f"Verification timed out; inspect {run}")
    if args.empty:
        print(f"Loader-only server exited {result.returncode}; inspect {run} for startup result", flush=True)
        return
    if result.returncode or not report.exists():
        raise SystemExit(f"Verification failed (exit={result.returncode}); inspect {run}")
    data = json.loads(report.read_text())
    if data.get("result") != "PASS" or data.get("cleanroom") != VERSION:
        raise SystemExit(f"Verification failed or wrong loader version: {report}")
    if args.mmce == "absent":
        loads = (run / "class-load.log").read_text()
        for forbidden in ("hellfirepvp.modularmachinery.", "github.kasuminova.mmce.",
                          "net.createmod.ponder.mmce.", "net.createmod.ponder.verification.mmce.MMCEIntegrationChecks"):
            if forbidden in loads:
                raise SystemExit(f"Optional MMCE class loaded while absent: {forbidden}")
        print("PASS: no MMCE implementation classes loaded", flush=True)
    if args.crafttweaker == "absent":
        loads = (run / "class-load.log").read_text()
        for forbidden in ("crafttweaker.", "stanhebben.zenscript.",
                          "net.createmod.ponder.compat.crafttweaker.PonderCraftTweakerCompat$Bridge"):
            if any(line.split("[class,load] ", 1)[-1].split(" source:", 1)[0].startswith(forbidden)
                   for line in loads.splitlines()):
                raise SystemExit(f"Optional CraftTweaker class loaded while absent: {forbidden}")
        print("PASS: no CraftTweaker, ZenScript or optional bridge classes loaded", flush=True)
    print("PASS:", report, flush=True)


if __name__ == "__main__":
    main()
