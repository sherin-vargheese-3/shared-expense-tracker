/**
 * Name to show in history. Former members are labelled so they can't be confused with a current
 * member who has the same name.
 */
export const displayName = (name: string, removed: boolean): string =>
  removed ? `${name} (former member)` : name
