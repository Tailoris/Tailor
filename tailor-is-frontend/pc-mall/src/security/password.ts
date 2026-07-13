export interface PasswordPolicy {
  minLength: number;
  maxLength: number;
  requireUppercase: boolean;
  requireLowercase: boolean;
  requireNumber: boolean;
  requireSpecialChar: boolean;
  specialChars: string;
}

export const DEFAULT_PASSWORD_POLICY: PasswordPolicy = {
  minLength: 8,
  maxLength: 32,
  requireUppercase: true,
  requireLowercase: true,
  requireNumber: true,
  requireSpecialChar: true,
  specialChars: '!@#$%^&*()_+-=[]{}|;:,.<>?',
};

export function validatePassword(
  password: string,
  policy: PasswordPolicy = DEFAULT_PASSWORD_POLICY
): { isValid: boolean; errors: string[] } {
  const errors: string[] = [];

  if (password.length < policy.minLength) {
    errors.push(`密码长度不能少于${policy.minLength}位`);
  }

  if (password.length > policy.maxLength) {
    errors.push(`密码长度不能超过${policy.maxLength}位`);
  }

  if (policy.requireUppercase && !/[A-Z]/.test(password)) {
    errors.push('密码必须包含至少一个大写字母');
  }

  if (policy.requireLowercase && !/[a-z]/.test(password)) {
    errors.push('密码必须包含至少一个小写字母');
  }

  if (policy.requireNumber && !/[0-9]/.test(password)) {
    errors.push('密码必须包含至少一个数字');
  }

  if (policy.requireSpecialChar && !new RegExp(`[${policy.specialChars}]`).test(password)) {
    errors.push(`密码必须包含至少一个特殊字符(${policy.specialChars})`);
  }

  if (/(\w)\1\1\1/.test(password)) {
    errors.push('密码不能包含连续4个相同字符');
  }

  if (password.includes('password') || password.includes('123456')) {
    errors.push('密码不能包含常见弱口令');
  }

  return {
    isValid: errors.length === 0,
    errors,
  };
}

export function getPasswordStrength(password: string): { score: number; label: string; color: string } {
  let score = 0;

  if (password.length >= 8) score += 1;
  if (password.length >= 12) score += 1;
  if (/[a-z]/.test(password)) score += 1;
  if (/[A-Z]/.test(password)) score += 1;
  if (/[0-9]/.test(password)) score += 1;
  if (/[^a-zA-Z0-9]/.test(password)) score += 1;
  if (password.length >= 16) score += 1;

  if (score <= 2) return { score, label: '弱', color: '#ef4444' };
  if (score <= 4) return { score, label: '中', color: '#f59e0b' };
  if (score <= 6) return { score, label: '强', color: '#22c55e' };
  return { score, label: '非常强', color: '#16a34a' };
}

export function generateToken(length: number = 32): string {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  const array = new Uint8Array(length);
  crypto.getRandomValues(array);
  return Array.from(array, (x) => chars[x % chars.length]).join('');
}

export function generateCode(length: number = 6): string {
  const chars = '0123456789';
  const array = new Uint8Array(length);
  crypto.getRandomValues(array);
  return Array.from(array, (x) => chars[x % chars.length]).join('');
}

export async function hashString(str: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(str);
  const hash = await crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(hash))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}
