import { apiToCents, centsToApi, centsToInput, equalSplit, formatCents, parseAmount } from './money'

describe('parseAmount', () => {
  it.each([
    ['12', 1200],
    ['12.5', 1250],
    ['12.50', 1250],
    [' 0.01 ', 1],
    ['0', 0],
    ['9999999999.99', 999999999999],
  ])('parses %j to %d cents', (input, cents) => {
    expect(parseAmount(input)).toBe(cents)
  })

  it.each(['', 'abc', '1.234', '-5', '1,000', '.5', '1.', '12345678901', '1e3'])(
    'rejects %j',
    (input) => {
      expect(parseAmount(input)).toBeNull()
    },
  )
})

describe('conversions', () => {
  it('round-trips API amounts without float drift', () => {
    expect(apiToCents(33.33)).toBe(3333)
    expect(apiToCents(0.07)).toBe(7)
    expect(apiToCents(-26.66)).toBe(-2666)
    expect(centsToApi(3334)).toBe(33.34)
    expect(centsToInput(5)).toBe('0.05')
  })

  it('formats with two decimals', () => {
    expect(formatCents(123450)).toBe('1,234.50')
    expect(formatCents(5)).toBe('0.05')
  })
})

describe('equalSplit', () => {
  it('matches the backend rule: leftover cents go to the lowest ids', () => {
    expect([...equalSplit(10000, [3, 1, 2])]).toEqual([
      [1, 3334],
      [2, 3333],
      [3, 3333],
    ])
  })

  it('always sums to the amount', () => {
    for (let amount = 0; amount < 500; amount += 7) {
      const shares = equalSplit(amount, [4, 9, 2, 11, 5])
      expect([...shares.values()].reduce((a, b) => a + b, 0)).toBe(amount)
    }
  })

  it('returns no shares for no members', () => {
    expect(equalSplit(100, []).size).toBe(0)
  })
})
