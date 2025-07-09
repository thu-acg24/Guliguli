export const formatTime = (timestamp: string | number, isAbbreviation = true) => {
    const now = new Date();
    const date = new Date(timestamp);

    // Check if it's today
    if (now.toDateString() === date.toDateString() && isAbbreviation) {
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    // Check if it was yesterday
    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);
    if (yesterday.toDateString() === date.toDateString() && isAbbreviation) {
        return `昨天 ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
    }

    // Check if it's the current year
    if (now.getFullYear() === date.getFullYear() && isAbbreviation) {
        return `${date.getMonth() + 1}月${date.getDate()}日 ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
    }

    // For previous years
    return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
};
export const dateformatTime = (timestamp: string | number) => {
    const now = new Date();
    const date = new Date(timestamp);

    // Check if it's the current year
    if (now.getFullYear() === date.getFullYear()) {
        return `${date.getMonth() + 1}-${date.getDate()}`;
    }
    // For previous years
    return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`;
};

export const formatDuration = (seconds: number): string => {
    const hours = Math.floor(seconds / 3600);
    seconds %= 3600;
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${hours > 0 ? `${hours}:` : ''}${mins}:${secs.toString().padStart(2, '0')}`;
};

export const formatCount = (count: number): string => {
    if (count >= 100000000) {
        return `${(count / 100000000).toFixed(1)}亿`;
    }
    if (count >= 10000) {
        return `${(count / 10000).toFixed(1)}万`;
    }
    return count.toString();
};
