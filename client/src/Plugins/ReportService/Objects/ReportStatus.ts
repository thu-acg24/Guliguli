export enum ReportStatus {
    pending = 'Pending',
    resolved = 'Resolved',
    rejected = 'Rejected'
}

export const reportStatusList = Object.values(ReportStatus)

export function getReportStatus(newType: string): ReportStatus {
    return reportStatusList.filter(t => t === newType)[0]
}
