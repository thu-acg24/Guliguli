import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useUserToken } from "Globals/GlobalStore";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { QueryDanmakuReportsMessage } from "Plugins/ReportService/APIs/QueryDanmakuReportsMessage";
import { ProcessDanmakuReportMessage } from "Plugins/ReportService/APIs/ProcessDanmakuReportMessage";
import { ReportDanmaku } from "Plugins/ReportService/Objects/ReportDanmaku";
import { ReportStatus } from "Plugins/ReportService/Objects/ReportStatus";
import { videoPagePath } from "Pages/VideoPage/VideoPage";
import { useTopSuccessToast } from "Components/TopSuccessToast/useTopSuccessToast";

const DanmakuReportManagement: React.FC = () => {
    const navigate = useNavigate();
    const userToken = useUserToken();
    const [reports, setReports] = useState<ReportDanmaku[]>([]);
    const [loading, setLoading] = useState(true);
    const { ToastComponent, showSuccess } = useTopSuccessToast();

    useEffect(() => {
        loadReports();
    }, []);

    const loadReports = async () => {
        if (!userToken) return;

        try {
            setLoading(true);
            const response = await new Promise<string>((resolve, reject) => {
                new QueryDanmakuReportsMessage(userToken).send(
                    (info: string) => resolve(info),
                    (error: string) => reject(new Error(error))
                );
            });

            const reportsData = JSON.parse(response) as ReportDanmaku[];
            setReports(reportsData);
        } catch (error) {
            console.error("获取弹幕举报失败", error);
            materialAlertError("加载失败", error instanceof Error ? error.message : "获取弹幕举报失败");
        } finally {
            setLoading(false);
        }
    };

    const handleReportAction = async (reportID: number, status: ReportStatus) => {
        if (!userToken) {
            materialAlertError("未登录", "请先登录");
            return;
        }

        const actionText = status === ReportStatus.resolved ? "通过举报" : "驳回举报";

        if (!confirm(`确定要${actionText}吗？`)) {
            return;
        }

        try {
            await new Promise<void>((resolve, reject) => {
                new ProcessDanmakuReportMessage(userToken, reportID, status).send(
                    (info: string) => resolve(),
                    (error: string) => reject(new Error(error))
                );
            });

            // 从列表中移除已处理的举报
            setReports(prev => prev.filter(report => report.reportID !== reportID));
            showSuccess(`${actionText}成功`);
        } catch (error) {
            console.error("处理举报失败", error);
            materialAlertError("操作失败", error instanceof Error ? error.message : `${actionText}失败`);
        }
    };

    const formatDate = (timestamp: number): string => {
        return new Date(timestamp).toLocaleString('zh-CN');
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
                <div className="report-list">
                    {reports.map(report => (
                        <div key={report.reportID} className="report-item">
                            <div className="report-item-header">
                                <div className="report-item-id">
                                    举报ID: #{report.reportID}
                                </div>
                                <div className="report-item-time">
                                    {formatDate(report.timestamp)}
                                </div>
                            </div>

                            <div className="report-item-content">
                                <div className="report-item-reason">
                                    <div className="report-reason-label">举报原因：</div>
                                    <div className="report-reason-text">{report.reason}</div>
                                </div>

                                <div className="report-target-info">
                                    <div className="report-target-label">被举报弹幕：</div>
                                    <div className="report-target-text">
                                        弹幕ID: {report.danmakuID} | 举报者ID: {report.reporterID}
                                    </div>
                                </div>
                            </div>                                <div className="report-item-actions">
                                <button
                                    className="report-btn report-btn-approve"
                                    onClick={() => handleReportAction(report.reportID, ReportStatus.resolved)}
                                >
                                    通过举报
                                </button>
                                <button
                                    className="report-btn report-btn-reject"
                                    onClick={() => handleReportAction(report.reportID, ReportStatus.rejected)}
                                >
                                    驳回举报
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
