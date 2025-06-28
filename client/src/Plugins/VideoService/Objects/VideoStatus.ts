export enum VideoStatus {
    pending = '待审核',
    approved = '审核通过',
    rejected = '审核未通过'
}

export const videoStatusList = Object.values(VideoStatus)

export function getVideoStatus(newType: string): VideoStatus {
    return videoStatusList.filter(t => t === newType)[0]
}
