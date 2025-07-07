export enum VideoStatus {
    pending = '待审核',
    approved = '审核通过',
    rejected = '审核未通过',
    uploading = '上传中',
    private = '公众不可见',
    broken = '上传失败'
}

export const videoStatusList = Object.values(VideoStatus)

export function getVideoStatus(newType: string): VideoStatus {
    return videoStatusList.filter(t => t === newType)[0]
}
