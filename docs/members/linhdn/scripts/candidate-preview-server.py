"""Local visual preview of MockMvc fixtures only; no application API or POST emulation."""
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
from pathlib import Path
from urllib.parse import urlsplit, unquote
import argparse

ROOT = Path(__file__).resolve().parents[4]

class PreviewHandler(SimpleHTTPRequestHandler):
    def translate_path(self, path):
        parts = unquote(urlsplit(path).path).strip('/').split('/')
        if parts[0] == 'preview' and len(parts) > 1 and parts[1] in ('empty', 'populated'):
            base = ROOT / 'target' / 'candidate-preview'
            file = base / parts[1] / 'index.html'
        elif parts[0] in ('css', 'js', 'fonts'):
            base = ROOT / 'src' / 'main' / 'resources' / 'static'
            file = base.joinpath(*parts)
        else:
            return str(ROOT / 'target' / 'candidate-preview' / 'not-found')
        file = file.resolve()
        if not file.is_relative_to(base.resolve()):
            return str(ROOT / 'target' / 'candidate-preview' / 'not-found')
        return str(file)

    def log_message(self, format, *args):
        pass

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, default=8772)
    options = parser.parse_args()
    print(f'MockMvc fixture preview: http://127.0.0.1:{options.port}/preview/populated/', flush=True)
    ThreadingHTTPServer(('127.0.0.1', options.port), PreviewHandler).serve_forever()
