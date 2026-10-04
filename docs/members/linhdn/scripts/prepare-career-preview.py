"""Copy static assets to opt-in MockMvc HTML snapshots for visual QA only."""
from pathlib import Path
import shutil

root = Path('target/career-db-preview')
if not (root / 'index.html').exists():
    raise SystemExit('Run CareerFlowTests and CareerReadOnlyTests with -Dcareer.preview=true first.')
for directory in ('css', 'js', 'fonts'):
    shutil.copytree(Path('src/main/resources/static') / directory, root / directory, dirs_exist_ok=True)
for source, target in [('authenticated.html', 'preview-authenticated.html'),
                       ('jobs/41/apply/index.html', 'preview-apply.html')]:
    shutil.copyfile(Path('target/career-preview') / source, root / target)
print('Prepared DB-backed public HTML and fictional authenticated/apply visual fixtures. No HTTP backend/authentication is provided by this static preview.')
