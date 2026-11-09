"""Prueba HTTP de JWT contra una instancia local de pruebas. Solo usa la biblioteca estandar."""
import argparse
import base64
import json
import time
import urllib.error
import urllib.request
import uuid
from http.cookies import SimpleCookie

parser = argparse.ArgumentParser()
parser.add_argument('--base-url', default='http://127.0.0.1:18089/api')
parser.add_argument('--expiracion-corta', action='store_true', help='La instancia debe usar access de 2 segundos')
parser.add_argument('--output')
args = parser.parse_args()
results = []

class Cliente:
    def __init__(self):
        self.cookies = {}

    def request(self, caso, method, path, expected, data=None, token=None, refresh=None, csrf=True):
        cookies = dict(self.cookies)
        if refresh is not None:
            cookies['refresh_token'] = refresh
        headers = {'Content-Type': 'application/json', 'Cookie': '; '.join(f'{k}={v}' for k,v in cookies.items())}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        if csrf and self.cookies.get('XSRF-TOKEN'):
            headers['X-XSRF-TOKEN'] = self.cookies['XSRF-TOKEN']
        request = urllib.request.Request(args.base_url + path, method=method, headers=headers,
                data=json.dumps(data).encode() if data is not None else (b'' if method=='POST' else None))
        try:
            response = urllib.request.urlopen(request, timeout=20)
        except urllib.error.HTTPError as error:
            response = error
        body = response.read()
        result = json.loads(body) if body else {}
        cookie_headers = response.headers.get_all('Set-Cookie') or []
        for raw in cookie_headers:
            cookie = SimpleCookie(raw)
            for name, value in cookie.items():
                if value['max-age'] == '0': self.cookies.pop(name, None)
                else: self.cookies[name] = value.value
        results.append({'case': caso, 'expected': expected, 'status': response.code, 'pass': response.code == expected})
        assert response.code == expected, (caso, response.code, result)
        return result, cookie_headers

    def csrf(self):
        self.request('obtener_csrf', 'GET', '/auth/csrf', 204)

def claims(token):
    return json.loads(base64.urlsafe_b64decode(token.split('.')[1]+'=='))

client = Cliente()
client.request('protegido_sin_access', 'GET', '/usuarios/me', 401)
client.request('refresh_sin_csrf', 'POST', '/auth/refresh', 403, csrf=False)
client.request('login_sin_csrf', 'POST', '/auth/login', 403, {}, csrf=False)
client.csrf()
email = str(uuid.uuid4())+'@example.com'
datos = {'nombreEmpresa':'JWT Smoke', 'nit':str(uuid.uuid4()), 'emailContacto':'contacto@example.com',
         'nombre':'Prueba JWT', 'email':email, 'password':'Original123!'}
session, cookies = client.request('registro', 'POST', '/auth/register', 201, datos)
assert 'refreshToken' not in session
assert any('HttpOnly' in c and 'SameSite=Strict' in c and 'Path=/api/auth' in c for c in cookies)
old_access = session['token']
old_refresh = client.cookies['refresh_token']
client.request('access_valido', 'GET', '/usuarios/me', 200, token=old_access)
client.request('refresh_no_es_access', 'GET', '/usuarios/me', 401, token=old_refresh)
updated, _ = client.request('rotacion', 'POST', '/auth/refresh', 200)
new_refresh = client.cookies['refresh_token']
assert new_refresh != old_refresh
assert claims(new_refresh)['exp'] == claims(old_refresh)['exp']
client.request('replay', 'POST', '/auth/refresh', 401, refresh=old_refresh)
client.request('descendiente_revocado', 'POST', '/auth/refresh', 401, refresh=new_refresh)
client.request('access_revocado_por_replay', 'GET', '/usuarios/me', 401, token=updated['token'])
session, _ = client.request('login', 'POST', '/auth/login', 200, {'email':email,'password':'Original123!'})
old_access = session['token']
old_refresh = client.cookies['refresh_token']
client.request('cambiar_password', 'PUT', f"/usuarios/{session['usuarioId']}", 200,
               {'password':'NuevaPassword123!'}, token=old_access)
client.request('access_revocado_por_password', 'GET', '/usuarios/me', 401, token=old_access)
client.request('refresh_revocado_por_password', 'POST', '/auth/refresh', 401, refresh=old_refresh)
client.request('password_anterior_rechazada', 'POST', '/auth/login', 401, {'email':email,'password':'Original123!'})
session, _ = client.request('password_nueva', 'POST', '/auth/login', 200, {'email':email,'password':'NuevaPassword123!'})
client.request('logout_solo_cookie', 'POST', '/auth/logout', 204)
client.request('logout_idempotente', 'POST', '/auth/logout', 204)
client.request('refresh_tras_logout', 'POST', '/auth/refresh', 401)
session, _ = client.request('login_para_expiracion', 'POST', '/auth/login', 200, {'email':email,'password':'NuevaPassword123!'})
if args.expiracion_corta:
    time.sleep(2.2)
    client.request('access_expirado', 'GET', '/usuarios/me', 401, token=session['token'])
    client.request('logout_access_expirado', 'POST', '/auth/logout', 204, token=session['token'])
    client.request('refresh_tras_logout_expirado', 'POST', '/auth/refresh', 401)
    session, _ = client.request('login_para_roles', 'POST', '/auth/login', 200, {'email':email,'password':'NuevaPassword123!'})

editor_email=str(uuid.uuid4())+'@example.com'
editor, _ = client.request('crear_editor', 'POST', '/usuarios', 201,
        {'nombre':'Editor', 'email':editor_email, 'password':'Password123!', 'rolAcceso':'EDITOR'}, token=session['token'])
other=Cliente()
other.csrf()
editor_session, _ = other.request('login_editor', 'POST', '/auth/login', 200, {'email':editor_email,'password':'Password123!'})
other.request('editor_sin_permiso_admin', 'GET', '/usuarios', 403, token=editor_session['token'])
client.request('desactivar_editor', 'DELETE', f"/usuarios/{editor['id']}", 204, token=session['token'])
other.request('inactivo_no_renueva', 'POST', '/auth/refresh', 401)
client.request('reactivar_editor', 'PUT', f"/usuarios/{editor['id']}/activar", 200, {}, token=session['token'])
other.request('reactivar_no_resucita_access', 'GET', '/usuarios/me', 401, token=editor_session['token'])
missing = str(uuid.uuid4())+'@example.com'
for intento in range(11):
    client.request('limite_login_'+str(intento+1), 'POST', '/auth/login', 401 if intento<10 else 429,
                   {'email':missing,'password':'NoExiste123!'})

report=json.dumps({'passed':len(results),'results':results}, indent=2)
if args.output:
    from pathlib import Path
    Path(args.output).write_text(report, encoding='utf-8')
print(report)
