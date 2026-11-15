export const passwordRequirements = 'Use 12–128 characters with uppercase, lowercase, a number and a symbol.';
const symbols = `^$*.[]{}()?"!@#%&/\\,><':;|_~`;
export function passwordChecks(password) {
  return [
    ['12–128 characters', password.length >= 12 && password.length <= 128],
    ['An uppercase letter', /[A-Z]/.test(password)],
    ['A lowercase letter', /[a-z]/.test(password)],
    ['A number', /[0-9]/.test(password)],
    ['A symbol', [...password].some(character => symbols.includes(character))]
  ];
}
export function requireStrongPassword(password) {
  if (!passwordChecks(password).every(([,valid]) => valid)) throw new Error(passwordRequirements);
}
