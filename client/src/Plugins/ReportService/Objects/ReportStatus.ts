export enum ReportStatus {
    pending = '待处理',
    resolved = '已处理',
    rejected = '驳回'
}

export const reportStatusList = Object.values(ReportStatus)

export function getReportStatus(newType: string): ReportStatus {
    return reportStatusList.filter(t => t === newType)[0]
}
