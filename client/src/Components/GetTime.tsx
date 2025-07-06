export const formatTime = (timestamp: string|number,isAbbreviation=true) => {
    const now = new Date();
    const date = new Date(timestamp);
    
    // Check if it's today
    if (now.toDateString() === date.toDateString()&&isAbbreviation) {
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    
    // Check if it was yesterday
    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);
    if (yesterday.toDateString() === date.toDateString()&&isAbbreviation) {
        return `昨天 ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
    }
    
    // Check if it's the current year
    if (now.getFullYear() === date.getFullYear()&&isAbbreviation) {
        return `${date.getMonth() + 1}月${date.getDate()}日 ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
    }
    
    // For previous years
    return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
};
