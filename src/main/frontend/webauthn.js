/*
 * Browser side of the WebAuthn ceremonies. Talks to the endpoints that Spring Security's
 * http.webAuthn() registers. Exposed as window.passkeys so Vaadin Flow can call it with executeJs.
 *
 * Every function takes the CSRF header (name + token) from the server, because the
 * WebAuthn endpoints are protected by Spring Security's CSRF filter.
 */

function toBase64Url(buffer) {
  const base64 = btoa(String.fromCharCode(...new Uint8Array(buffer)));
  return base64.replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
}

function fromBase64Url(value) {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/');
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

async function post(url, csrf, body) {
  const response = await fetch(url, {
    method: 'POST',
    credentials: 'same-origin',
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
    publicKey: {
      ...options,
      challenge: fromBase64Url(options.challenge),
      user: { ...options.user, id: fromBase64Url(options.user.id) },
      excludeCredentials: (options.excludeCredentials ?? []).map((c) => ({ ...c, id: fromBase64Url(c.id) })),
    },
  });

  const { response } = credential;
  const result = await post('webauthn/register', csrf, {
    publicKey: {
      label,
      credential: {
        id: credential.id,
        rawId: toBase64Url(credential.rawId),
        type: credential.type,
        response: {
          attestationObject: toBase64Url(response.attestationObject),
          clientDataJSON: toBase64Url(response.clientDataJSON),
          transports: response.getTransports ? response.getTransports() : [],
        },
        clientExtensionResults: credential.getClientExtensionResults(),
        authenticatorAttachment: credential.authenticatorAttachment,
      },
    },
  });
  if (!result.success) {
    throw new Error('The server rejected the passkey');
  }
  return true;
}

/** Signs in with a passkey and returns the URL to continue to. */
async function authenticate(csrf) {
  const options = await post('webauthn/authenticate/options', csrf);

  const credential = await navigator.credentials.get({
    publicKey: {
      ...options,
      challenge: fromBase64Url(options.challenge),
      allowCredentials: (options.allowCredentials ?? []).map((c) => ({ ...c, id: fromBase64Url(c.id) })),
    },
  });

  const { response } = credential;
  const result = await post('login/webauthn', csrf, {
    id: credential.id,
    rawId: toBase64Url(credential.rawId),
    credType: credential.type,
    response: {
      authenticatorData: toBase64Url(response.authenticatorData),
      clientDataJSON: toBase64Url(response.clientDataJSON),
      signature: toBase64Url(response.signature),
      userHandle: response.userHandle ? toBase64Url(response.userHandle) : undefined,
    },
    clientExtensionResults: credential.getClientExtensionResults(),
    authenticatorAttachment: credential.authenticatorAttachment,
  });
  if (!result.authenticated) {
    throw new Error('Passkey sign-in failed');
  }
  return result.redirectUrl;
}

function isSupported() {
  return !!(window.PublicKeyCredential && navigator.credentials);
}

window.passkeys = { register, authenticate, isSupported };
