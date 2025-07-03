
export const formatTime = (timestamp: string) => {
    const now = new Date();
    const date = new Date(timestamp);
    const diffDays = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else if (diffDays === 1) {
    return '昨天';
    } else if (diffDays < 7) {
    return `${diffDays}天前`;
    } else {
    return date.toLocaleDateString([], { month: 'numeric', day: 'numeric' });
    }
};