const formatter = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' })

export const formatDateTime = (iso: string): string => formatter.format(new Date(iso))
