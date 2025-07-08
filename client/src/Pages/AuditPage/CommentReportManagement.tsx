import React, { useState, useEffect } from 'react';
import { useUserToken } from 'Globals/GlobalStore';
import { materialAlertError } from 'Plugins/CommonUtils/Gadgets/AlertGadget';
import { ReportComment } from 'Plugins/ReportService/Objects/ReportComment';
import { ReportStatus } from 'Plugins/ReportService/Objects/ReportStatus';
import { Comment } from 'Plugins/CommentService/Objects/Comment';
import { QueryCommentReportsMessage } from 'Plugins/ReportService/APIs/QueryCommentReportsMessage';
import { ProcessCommentReportMessage } from 'Plugins/ReportService/APIs/ProcessCommentReportMessage';
import { QueryCommentByIDMessage } from 'Plugins/CommentService/APIs/QueryCommentByIDMessage';
import { useTopSuccessToast } from "Components/TopSuccessToast/useTopSuccessToast";

const CommentReportManagement: React.FC = () => {
    const userToken = useUserToken();
    const [reports, setReports] = useState<ReportComment[]>([]);
    const [commentDetails, setCommentDetails] = useState<{ [key: number]: Comment }>({});
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
                new QueryCommentReportsMessage(userToken).send(
                    (info: string) => resolve(info),
                    (error: string) => reject(new Error(error))
                );
            });

            const responseData = JSON.parse(response);
            if (responseData.reports) {
                const reportsList = responseData.reports.map((report: any) =>
                    new ReportComment(
                        report.reportID,
                        report.commentID,
                        report.reporterID,
                        report.reason,
                        report.status,
                        report.timestamp
                    )
                );
                setReports(reportsList);

                // 加载评论详情
                await loadCommentDetails(reportsList);
            }
        } catch (error) {
            console.error('加载评论举报失败:', error);
            materialAlertError('加载失败', error instanceof Error ? error.message : '获取评论举报失败');
        } finally {
            setLoading(false);
        }
    };

    const loadCommentDetails = async (reportsList: ReportComment[]) => {
        const commentIDs = [...new Set(reportsList.map(report => report.commentID))];
        const details: { [key: number]: Comment } = {};

        for (const commentID of commentIDs) {
            try {
                const response = await new Promise<string>((resolve, reject) => {
                    new QueryCommentByIDMessage(commentID).send(
                        (info: string) => resolve(info),
                        (error: string) => reject(new Error(error))
                    );
                });

                const responseData = JSON.parse(response);
                if (responseData.comment) {
                    const comment = responseData.comment;
                    details[commentID] = new Comment(
                        comment.commentID,
                        comment.content,
                        comment.videoID,
                        comment.authorID,
                        comment.replyToID,
                        comment.replyToUserID,
                        comment.likes,
                        comment.replyCount,
                        comment.timestamp
                    );
                }
            } catch (error) {
                console.error(`加载评论 ${commentID} 详情失败:`, error);
            }
        }

        setCommentDetails(details);
    };

    const handleReportAction = async (reportID: number, newStatus: ReportStatus) => {
        if (!userToken) {
            materialAlertError('未登录', '请先登录');
            return;
        }

        try {
            await new Promise<string>((resolve, reject) => {
                new ProcessCommentReportMessage(userToken, reportID, newStatus).send(
                    (info: string) => resolve(info),
                    (error: string) => reject(new Error(error))
                );
            });

            setReports(prev =>
                prev.map(report =>
                    report.reportID === reportID
                        ? new ReportComment(
                            report.reportID,
                            report.commentID,
                            report.reporterID,
                            report.reason,
                            newStatus,
                            report.timestamp
                        )
                        : report
                )
            );

            showSuccess('举报状态已更新');
        } catch (error) {
            console.error('处理举报失败:', error);
            materialAlertError('操作失败', error instanceof Error ? error.message : '处理举报失败');
        }
    };

    const formatDate = (timestamp: number) => {
        return new Date(timestamp * 1000).toLocaleString();
    };

    const truncateText = (text: string, maxLength: number = 50) => {
        return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
    };

    if (loading) {
        return <div className="loading">加载中...</div>;
    }

    const pendingReports = reports.filter(report => report.status === ReportStatus.pending);

    return (
        <div className="audit-content">
            {ToastComponent}
            <h2>评论举报管理</h2>

            <div className="stats">
                <div className="stat-item">
                    <span className="stat-label">待处理举报:</span>
                    <span className="stat-value">{pendingReports.length}</span>
                </div>
            </div>

            <div className="report-list">
                {pendingReports.length === 0 ? (
                    <div className="no-data">暂无待处理的评论举报</div>
                ) : (
                    pendingReports.map(report => {
                        const comment = commentDetails[report.commentID];

                        return (
                            <div key={report.reportID} className="report-item">
                                <div className="report-header">
                                    <span className="report-id">举报ID: {report.reportID}</span>
                                    <span className="report-time">{formatDate(report.timestamp)}</span>
                                </div>

                                <div className="report-content">
                                    <div className="report-info">
                                        <div className="info-row">
                                            <strong>举报原因:</strong> {report.reason}
                                        </div>
                                        <div className="info-row">
                                            <strong>举报人ID:</strong> {report.reporterID}
                                        </div>
                                        <div className="info-row">
                                            <strong>评论ID:</strong> {report.commentID}
                                        </div>
                                    </div>

                                    {comment && (
                                        <div className="comment-preview">
                                            <div className="comment-header">
                                                <strong>被举报评论内容:</strong>
                                            </div>
                                            <div className="comment-content">
                                                <div className="comment-text">
                                                    {truncateText(comment.content, 100)}
                                                </div>
                                                <div className="comment-meta">
                                                    <span>作者ID: {comment.authorID}</span>
                                                    <span>视频ID: {comment.videoID}</span>
                                                    <span>点赞数: {comment.likes}</span>
                                                    <span>发布时间: {new Date(comment.timestamp).toLocaleString()}</span>
                                                </div>
                                            </div>
                                        </div>
                                    )}
                                </div>

                                <div className="report-actions">
                                    <button
                                        className="btn-approve"
                                        onClick={() => handleReportAction(report.reportID, ReportStatus.resolved)}
                                    >
                                        通过举报
                                    </button>
                                    <button
                                        className="btn-reject"
                                        onClick={() => handleReportAction(report.reportID, ReportStatus.rejected)}
                                    >
                                        驳回举报
                                    </button>
                                </div>
                            </div>
                        );
                    })
                )}
            </div>
        </div>
    );
};

export default CommentReportManagement;
