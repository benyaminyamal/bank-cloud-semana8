#!/usr/bin/env python3
"""Panel local. Sirve la página y reenvía las llamadas al gateway y al servidor OAuth2."""

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import pathlib
import urllib.error
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent
GATEWAY = "http://localhost:18080"
AUTH = "http://localhost:9000"


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        path = self.path.split("?", 1)[0]
        if path.startswith("/api/"):
            self._proxy(GATEWAY + self.path)
            return
        if path in ("/", "/index.html"):
            body = (ROOT / "index.html").read_bytes()
            self._send(200, "text/html; charset=utf-8", body)
            return
        self.send_error(404)

    def do_POST(self):
        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length)
        path = self.path.split("?", 1)[0]
        if path == "/oauth/token":
            self._proxy(AUTH + "/oauth2/token", body, "application/x-www-form-urlencoded")
            return
        if path.startswith("/api/"):
            self._proxy(GATEWAY + path, body, self.headers.get("Content-Type", "application/json"))
            return
        self.send_error(404)

    def _proxy(self, url, body=None, content_type=None):
        request = urllib.request.Request(url, data=body, method="POST" if body is not None else "GET")
        if content_type and body is not None:
            request.add_header("Content-Type", content_type)
        authorization = self.headers.get("Authorization")
        if authorization:
            request.add_header("Authorization", authorization)
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                payload = response.read()
                self._send(response.status, response.headers.get("Content-Type", "application/json"), payload)
        except urllib.error.HTTPError as error:
            payload = error.read()
            content_type = error.headers.get("Content-Type", "application/json") if error.headers else "application/json"
            self._send(error.code, content_type, payload)

    def _send(self, status, content_type, payload):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)


if __name__ == "__main__":
    server = ThreadingHTTPServer(("127.0.0.1", 8090), Handler)
    print("Panel en http://localhost:8090")
    server.serve_forever()
