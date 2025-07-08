export enum VideoStatus {
    pending = 'Pending',
    approved = 'Approved',
    rejected = 'Rejected',
    uploading = 'Uploading',
    private = 'Private',
    broken = 'Broken',
}

export const videoStatusList = Object.values(VideoStatus)

export function getVideoStatus(newType: string): VideoStatus {
    return videoStatusList.filter(t => t === newType)[0]
}
