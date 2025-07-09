import React, { useState, useEffect } from "react";
import { useNavigateVideo } from "Globals/Navigate";
import { useUserToken } from "Globals/GlobalStore";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { ReportComment } from "Plugins/ReportService/Objects/ReportComment";
import { ReportStatus } from "Plugins/ReportService/Objects/ReportStatus";
import { Comment } from "Plugins/CommentService/Objects/Comment";
import { QueryCommentReportsMessage } from "Plugins/ReportService/APIs/QueryCommentReportsMessage";
import { ProcessCommentReportMessage } from "Plugins/ReportService/APIs/ProcessCommentReportMessage";
import { QueryCommentByIDMessage } from "Plugins/CommentService/APIs/QueryCommentByIDMessage";
import { useTopSuccessToast } from "Components/TopSuccessToast/useTopSuccessToast";
import { formatTime } from "Components/Formatter";

const CommentReportManagement: React.FC = () => {
    interface ReportWithComment {
        report: ReportComment;
        comment: Comment;
    }

    const { navigateVideo } = useNavigateVideo();
    const userToken = useUserToken();
    const [reports, setReports] = useState<ReportWithComment[]>([]);
    const [loading, setLoading] = useState(true);
    const { ToastComponent, showSuccess } = useTopSuccessToast();

    useEffect(() => {
        loadReports();
    }, []);

    const loadReports = async () => {
        try {
            setLoading(true);
            const response = await new Promise<string>((resolve, reject) => {
                new QueryCommentReportsMessage(userToken).send(
                    (info: string) => resolve(info),
                    (error: string) => reject(new Error(error))
                );
            });

            const reportsData = JSON.parse(response) as ReportComment[];

            // 获取每个举报对应的评论内容
            const reportsWithComment = await Promise.all(
                reportsData.map(async (report) => {
                    const commentResponse = await new Promise<string>((resolve, reject) => {
                        new QueryCommentByIDMessage(report.commentID).send(
                            (info: string) => resolve(info),
                            (error: string) => reject(new Error(error))
                        );
                    });
                    const comment = JSON.parse(commentResponse) as Comment;
                    return { report, comment };
                })
            );

            setReports(reportsWithComment);
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
                new ProcessCommentReportMessage(userToken, reportID, status).send(
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
                    <h1 className="report-title">评论举报管理</h1>
                    <p className="report-subtitle">加载中...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="report-management">
            {ToastComponent}
            <div className="report-header">
                <h1 className="report-title">评论举报管理</h1>
                <p className="report-subtitle">
                    共有 {reports.length} 个待处理举报
                </p>
            </div>

            {reports.length === 0 ? (
                <div className="report-empty">
                    <div className="report-empty-icon">💬</div>
                    <div className="report-empty-text">暂无待处理的评论举报</div>
                </div>
            ) : (
                <div className="danmaku-report-list">
                    {reports.map(item => (
                        <div key={item.report.reportID} className="danmaku-report-item">
                            <div className="danmaku-report-content">
                                <div className="danmaku-report-reason">
                                    <strong>举报评论：</strong>{item.comment.content}
                                </div>
                                <div className="danmaku-report-reason">
                                    <strong>举报原因：</strong>{item.report.reason}
                                </div>
                                <div className="danmaku-report-meta">
                                    举报时间：{formatTime(item.report.timestamp, false)}
                                </div>
                            </div>

                            <div className="danmaku-report-actions">
                                <button
                                    className="danmaku-action-btn danmaku-action-view"
                                    onClick={() => handleViewVideo(item.comment.videoID)}
                                    title="查看原视频"
                                >
                                    查看原视频
                                </button>
                                <button
                                    className="danmaku-action-btn danmaku-action-approve"
                                    onClick={() => handleReportAction(item.report.reportID, ReportStatus.resolved)}
                                    title="通过举报"
                                >
                                    ✓
                                </button>
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

export default CommentReportManagement;
