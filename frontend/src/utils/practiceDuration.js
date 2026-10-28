export function formatPracticeDuration(totalSeconds) {
  const seconds = Number(totalSeconds);
  const minutes = Number.isFinite(seconds)
    ? Math.max(0, Math.floor(seconds / 60))
    : 0;
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;

  if (hours === 0) {
    return `${remainingMinutes} min`;
  }

  const hourLabel = hours === 1 ? 'hr' : 'hrs';
  return remainingMinutes === 0
    ? `${hours} ${hourLabel}`
    : `${hours} ${hourLabel} ${remainingMinutes} min`;
}
