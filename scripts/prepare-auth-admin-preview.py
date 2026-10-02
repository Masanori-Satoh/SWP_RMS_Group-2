"""Prepare opt-in MockMvc fixtures; provides no backend, login, or database writes."""
from pathlib import Path
import shutil

root = Path('target/auth-admin-preview')
if not (root / 'preview/login/index.html').exists():
    raise SystemExit('Run SecurityFlowTests with -Dui.preview=true first.')
for directory in ('css', 'js', 'fonts'):
    shutil.copytree(Path('src/main/resources/static') / directory, root / directory, dirs_exist_ok=True)
# Previous SELECT-only rendered public homepage, when available, checks shared brand/tokens.
public_page = Path('target/career-db-preview/index.html')
if public_page.exists():
    (root / 'preview/careers').mkdir(parents=True, exist_ok=True)
    shutil.copyfile(public_page, root / 'preview/careers/index.html')
print('Prepared fictional Auth/Admin MockMvc fixtures with local assets. Static preview cannot process forms.')
