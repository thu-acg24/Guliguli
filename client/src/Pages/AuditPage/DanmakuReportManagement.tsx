import React, { useState, useEffect } from "react";
import { useNavigateVideo } from "Globals/Navigate";
import { useUserToken } from "Globals/GlobalStore";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { QueryDanmakuReportsMessage } from "Plugins/ReportService/APIs/QueryDanmakuReportsMessage";
import { ProcessDanmakuReportMessage } from "Plugins/ReportService/APIs/ProcessDanmakuReportMessage";
import { QueryDanmakuByIDMessage } from "Plugins/DanmakuService/APIs/QueryDanmakuByIDMessage";
import { ReportDanmaku } from "Plugins/ReportService/Objects/ReportDanmaku";
import { Danmaku } from "Plugins/DanmakuService/Objects/Danmaku";
import { ReportStatus } from "Plugins/ReportService/Objects/ReportStatus";
import { useTopSuccessToast } from "Components/TopSuccessToast/useTopSuccessToast";
import { formatTime, formatDuration } from "Components/Formatter";

const DanmakuReportManagement: React.FC = () => {
    interface ReportWithDanmaku {
        report: ReportDanmaku;
        danmaku: Danmaku | null;
    }

    const { navigateVideo } = useNavigateVideo();
    const userToken = useUserToken();
    const [reports, setReports] = useState<ReportWithDanmaku[]>([]);
    const [loading, setLoading] = useState(true);
    const { ToastComponent, showSuccess } = useTopSuccessToast();

    useEffect(() => {
        loadReports();
    }, []);

    const loadReports = async () => {
        try {
            setLoading(true);
            const response = await new Promise<string>((resolve, reject) => {
                new QueryDanmakuReportsMessage(userToken).send(
                    (info: string) => resolve(info),
                    (error: string) => reject(new Error(error))
                );
            });

            const reportsData = JSON.parse(response) as ReportDanmaku[];

            // 获取每个举报对应的弹幕内容
            const reportsWithDanmaku = await Promise.all(
                reportsData.map(async (report) => {
                    try {
                        const danmakuResponse = await new Promise<string>((resolve, reject) => {
                            new QueryDanmakuByIDMessage(report.danmakuID).send(
                                (info: string) => resolve(info),
                                (error: string) => reject(new Error(error))
                            );
                        });
                        const danmaku = JSON.parse(danmakuResponse) as Danmaku;
                        return {report, danmaku};
                    } catch (e) {
                        console.warn(`获取弹幕 ${report.danmakuID} 信息失败:`, e)
                        const defaultDanmaku = new Danmaku(report.danmakuID, "弹幕信息获取失败", 0, 0, "#000000", 0);
                        return {report, danmaku: defaultDanmaku};
                    }
                })
            );

            setReports(reportsWithDanmaku);
        } catch (error) {
            console.error("获取弹幕举报失败", error);
            materialAlertError("加载失败", error instanceof Error ? error.message : "获取弹幕举报失败");
        } finally {
            setLoading(false);
        }
    };

    const handleReportAction = async (reportID: number, status: ReportStatus) => {
        try {
            await new Promise<void>((resolve, reject) => {
                new ProcessDanmakuReportMessage(userToken, reportID, status).send(
                    (info: string) => resolve(),
                    (error: string) => reject(new Error(error))
                );
            });

            // 从列表中移除已处理的举报
            setReports(prev => prev.filter(item => item.report.reportID !== reportID));
            showSuccess(`处理成功`);
        } catch (error) {
            console.error("处理举报失败", error);
            materialAlertError("操作失败", error instanceof Error ? error.message : ``);
        }
    };

    const handleViewVideo = (videoID: number) => {
        navigateVideo(videoID);
    };

    if (loading) {
        return (
            <div className="report-management">
                <div className="report-header">
                    <h1 className="report-title">弹幕举报管理</h1>
                    <p className="report-subtitle">加载中...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="report-management">
            {ToastComponent}
            <div className="report-header">
                <h1 className="report-title">弹幕举报管理</h1>
                <p className="report-subtitle">
                    共有 {reports.length} 个待处理举报
                </p>
            </div>

            {reports.length === 0 ? (
                <div className="report-empty">
                    <div className="report-empty-icon">💬</div>
                    <div className="report-empty-text">暂无待处理的弹幕举报</div>
                </div>
            ) : (
                <div className="danmaku-report-list">
                    {reports.map(item => (
                        <div key={item.report.reportID} className="danmaku-report-item">
                            <div className="danmaku-report-content">
                                <div className="danmaku-report-reason">
                                    <strong>举报弹幕：</strong>{item.danmaku.content}
                                </div>
                                <div className="danmaku-report-reason">
                                    <strong>弹幕时间：</strong>{formatDuration(item.danmaku.timeInVideo)}
                                </div>
                                <div className="danmaku-report-reason">
                                    <strong>举报原因：</strong>{item.report.reason}
                                </div>
                                <div className="danmaku-report-meta">
                                    举报时间：{formatTime(item.report.timestamp, false)}
                                </div>
                            </div>

                            <div className="danmaku-report-actions">
                                {
                                    item.danmaku.videoID > 0 &&
                                    <button
                                        className="danmaku-action-btn danmaku-action-view"
                                        onClick={() => handleViewVideo(item.danmaku.videoID)}
                                        title="查看原视频"
                                    >
                                        查看原视频
                                    </button>
                                }
                                {
                                    item.danmaku.videoID > 0 &&
                                    <button
                                        className="danmaku-action-btn danmaku-action-approve"
                                        onClick={() => handleReportAction(item.report.reportID, ReportStatus.resolved)}
                                        title="通过举报"
                                    >
                                        ✓
                                    </button>
                                }
                                <button
                                    className="danmaku-action-btn danmaku-action-reject"
                                    onClick={() => handleReportAction(item.report.reportID, ReportStatus.rejected)}
                                    title="驳回举报"
                                >
                                    ×
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default DanmakuReportManagement;
