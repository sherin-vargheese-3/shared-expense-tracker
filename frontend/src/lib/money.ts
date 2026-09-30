/**
 * Money helpers. Inside the app all arithmetic is done on integer cents, mirroring the backend,
 * so totals are exact. The API exchanges decimal amounts with two fraction digits.
 */

// Up to 10 integer digits and 2 fraction digits, matching the backend's @Digits constraint.
const AMOUNT_PATTERN = /^\d{1,10}(\.\d{1,2})?$/

/** Parses user input such as "12", "12.5" or "12.50" into cents; returns null when invalid. */
export function parseAmount(input: string): number | null {
  const trimmed = input.trim()
  if (!AMOUNT_PATTERN.test(trimmed)) {
    return null
  }
  const [whole, fraction = ''] = trimmed.split('.')
  return Number(whole) * 100 + Number(fraction.padEnd(2, '0'))
}

/** Converts an amount received from the API (e.g. 33.34) to cents. */
export const apiToCents = (amount: number): number => Math.round(amount * 100)

/** Converts cents to the decimal number the API expects (e.g. 3334 -> 33.34). */
export const centsToApi = (cents: number): number => Number((cents / 100).toFixed(2))

/** Cents as a plain, locale-independent input value (e.g. 3334 -> "33.34"). */
export const centsToInput = (cents: number): string => (cents / 100).toFixed(2)

const formatter = new Intl.NumberFormat(undefined, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

/** Formats cents for display, e.g. 123450 -> "1,234.50". */
export const formatCents = (cents: number): string => formatter.format(cents / 100)

/** Formats an API amount for display. */
export const formatAmount = (amount: number): string => formatCents(apiToCents(amount))

/**
 * Splits cents evenly, giving leftover cents to the lowest member ids first. This mirrors the
 * backend's rule so the preview matches what will be stored.
 */
export function equalSplit(amountCents: number, memberIds: number[]): Map<number, number> {
  const ids = [...memberIds].sort((a, b) => a - b)
  const shares = new Map<number, number>()
  if (ids.length === 0) {
    return shares
  }
  const base = Math.floor(amountCents / ids.length)
  const remainder = amountCents % ids.length
  ids.forEach((id, index) => shares.set(id, base + (index < remainder ? 1 : 0)))
  return shares
}
