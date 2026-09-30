#!/usr/bin/env node
// Seeds a demo group through the public API.
// Usage: node scripts/seed-demo.mjs [baseUrl]   (default http://localhost:3000)

const base = (process.argv[2] ?? 'http://localhost:3000').replace(/\/$/, '')

async function post(path, body) {
  const response = await fetch(`${base}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  const json = await response.json()
  if (!response.ok) {
    throw new Error(`${path} failed (${response.status}): ${JSON.stringify(json)}`)
  }
  return json
}

const group = await post('/api/groups', {
  name: 'Goa trip',
  members: ['Alice', 'Bob', 'Carol', 'Dan'],
})
const [alice, bob, carol, dan] = group.members.map((m) => m.id)
const expenses = `/api/groups/${group.id}/expenses`

await post(expenses, {
  description: 'Beach house (3 nights)',
  amount: 400,
  paidBy: alice,
  splitType: 'EQUAL',
  splits: [alice, bob, carol, dan].map((memberId) => ({ memberId })),
})
await post(expenses, {
  description: 'Seafood dinner',
  amount: 100,
  paidBy: bob,
  splitType: 'EXACT',
  splits: [
    { memberId: carol, amount: 60 },
    { memberId: dan, amount: 40 },
  ],
})
await post(expenses, {
  description: 'Taxi from the airport',
  amount: 10,
  paidBy: carol,
  splitType: 'EQUAL',
  splits: [alice, bob, carol].map((memberId) => ({ memberId })),
})
await post(`/api/groups/${group.id}/settlements`, {
  fromMemberId: dan,
  toMemberId: alice,
  amount: 50,
})

console.log(`Seeded "${group.name}": ${base}/groups/${group.id}`)
