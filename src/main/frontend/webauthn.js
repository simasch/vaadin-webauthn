/*
 * Browser side of the WebAuthn ceremonies. Talks to the endpoints that Spring Security's
 * http.webAuthn() registers. Exposed as window.passkeys so Vaadin Flow can call it with executeJs.
 *
 * Uses the WebAuthn Level 3 JSON helpers (PublicKeyCredential.parse*OptionsFromJSON and
 * credential.toJSON()) to convert between the server's base64url JSON and the browser's binary API.
 * Supported by Chrome/Edge 129+, Firefox 119+ and Safari 18.4+.
 *
 * Every function takes the CSRF header (name + token) from the server, because the
 * WebAuthn endpoints are protected by Spring Security's CSRF filter.
 */

async function post(url, csrf, body) {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) {
    throw new Error(`${url} answered HTTP ${response.status}`);
  }
  return response.json();
}

/** Creates a new passkey for the signed-in user and stores it under the given label. */
async function register(csrf, label) {
  const options = await post('webauthn/register/options', csrf);
  const credential = await navigator.credentials.create({
    publicKey: PublicKeyCredential.parseCreationOptionsFromJSON(options),
  });
  const result = await post('webauthn/register', csrf, { publicKey: { label, credential: credential.toJSON() } });
  if (!result.success) {
    throw new Error('The server rejected the passkey');
  }
  return true;
}

/** Signs in with a passkey and returns the URL to continue to. */
async function authenticate(csrf) {
  const options = await post('webauthn/authenticate/options', csrf);
  const credential = await navigator.credentials.get({
    publicKey: PublicKeyCredential.parseRequestOptionsFromJSON(options),
  });
  const result = await post('login/webauthn', csrf, credential.toJSON());
  if (!result.authenticated) {
    throw new Error('Passkey sign-in failed');
  }
  return result.redirectUrl;
}

window.passkeys = { register, authenticate };
