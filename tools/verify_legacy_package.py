#!/usr/bin/env python3
"""Check release metadata; this does not replace runtime compatibility acceptance."""
import argparse
import json
import hashlib
from pathlib import Path
from zipfile import ZipFile
p = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--require-verified-hash', action='store_true',
                    help='Refuse release unless this jar matches the runtime-accepted SHA-256')
args = parser.parse_args()
identity = json.loads((p/'legacy-identity.json').read_text())
jars = [f for f in (p/'build/libs').glob(identity['modid']+'-*.jar') if not f.stem.endswith(('-sources','-api','-mcp','-javadoc'))]
if len(jars) != 1:
    raise SystemExit('Expected exactly one production jar: '+repr(jars))
with ZipFile(jars[0]) as archive:
    metadata = json.loads(archive.read('mcmod.info'))[0]
    assert metadata['modid'] == identity['modid'], metadata
    assert metadata['name'] == identity['name'], metadata
    assert metadata['authorList'] == ['yudream'], metadata
    logo = metadata['logoFile']
    assert logo in archive.namelist(), 'Missing project icon: ' + logo
    assert archive.read(logo) == (p/'branding/icon.png').read_bytes(), 'Packaged icon differs from project artwork'
    assert '${' not in archive.read('mcmod.info').decode(), 'Unexpanded metadata'
    assert not any('/dev/' in name or 'Smoke.class' in name for name in archive.namelist() if name.endswith('.class')), 'Development harness in release'
    classes = [name for name in archive.namelist() if name.endswith('.class')]
    assert not any(name.startswith(('crafttweaker/', 'stanhebben/zenscript/',
                                    'hellfirepvp/modularmachinery/', 'github/kasuminova/mmce/', 'neofontrender/',
                                    'net/createmod/ponder/verification/')) for name in classes), 'Dependency or harness bundled in release'
    assert 'net/createmod/ponder/mmce/PonderMMCEMod.class' not in classes, 'Old standalone MMCE entrypoint bundled'
    assert 'net/createmod/ponder/mmce/PonderMMCEEntrypoint.class' in classes, 'MMCE optional integration missing'
    assert 'net/createmod/ponder/compat/revoui/PonderRevoMixinLoader.class' in classes, 'Optional Revo UI loader missing'
    assert 'mixins.ponder.revo_ui.json' in archive.namelist(), 'Optional Revo UI mixin config missing'
    assert 'META-INF/licenses/guideme-markdown-MIT.txt' in archive.namelist(), 'GuideME Markdown license missing'
    assert 'net/createmod/ponder/guide/MarkdownTutorialCompiler.class' in classes, 'Shared guide compiler missing'
    assert 'net/createmod/ponder/client/guide/MarkdownMediaScreen.class' in classes, 'Markdown media insertion panel missing'
    assert 'net/createmod/ponder/client/guide/MediaFileBrowserScreen.class' in classes, 'Game-native media browser missing'
    assert 'net/createmod/ponder/client/projection/ProjectionLayoutScreen.class' in classes, 'Material layout editor missing'
    assert 'net/createmod/ponder/client/projection/ProjectionPlacementEvents.class' in classes, 'World placement controls missing'
    assert 'net/createmod/ponder/foundation/element/StructureEntityElement.class' in classes, 'Captured entity timeline support missing'
    assert 'net/createmod/ponder/api/theme/PonderTheme.class' in classes, 'Theme API missing'
    assert 'net/createmod/ponder/client/theme/PonderThemeCommand.class' in classes, 'Theme selection command missing'
    assert 'net/createmod/ponder/script/ScriptTheme.class' in classes, 'Optional ZenScript theme API missing'
    for theme in ('classic', 'modern'):
        assert json.loads(archive.read(f'assets/ponder/themes/{theme}.json')), 'Theme resource missing: ' + theme
    assert not any(name.startswith(('com/feed_the_beast/', 'dev/yudream/mediaapi/')) for name in classes), 'Optional media dependency bundled'
    assert not any(':ftbquests' in dep for dep in metadata['dependencies']), 'Lazy FTB Quests bridge must not constrain reverse load ordering'
    assert 'after:media_api@[1.0.2,2.0.0)' in metadata['dependencies'], 'Media API must remain optional'
    assert archive.namelist().count('mixins.ponder.refmap.json') == 1, 'Expected one SRG refmap'
    assert 'after:crafttweaker@[4.1.20,)' in metadata['dependencies'], 'CraftTweaker must remain optional'
    assert not any('required' in dep and ':crafttweaker' in dep for dep in metadata['dependencies']), 'CraftTweaker declared required'
    cleanmix_versions = json.loads(archive.read('cleanmix_version_compatibility.json'))
    assert list(cleanmix_versions) == sorted(cleanmix_versions), 'CleanMix compatibility metadata must be sorted for reproducible jars'
    expected_tag = 'v' + metadata['version']
    assert identity['release_tag'] == expected_tag, 'Release tag differs from packaged version'
sha256 = hashlib.sha256(jars[0].read_bytes()).hexdigest()
if args.require_verified_hash and sha256 != identity['verified_jar_sha256']:
    raise SystemExit('Runtime acceptance required for new jar SHA-256: ' + sha256
                     + '\nAccepted: ' + identity['verified_jar_sha256'])
print('PASS:', jars[0].name, metadata['modid'], 'SHA-256:', sha256)
