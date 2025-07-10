import React, { useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import Header from "Components/Header/Header";
import SearchVideosTab from "./SearchVideosTab";
import SearchUsersTab from "./SearchUsersTab";
import "./SearchPage.css";

export const searchPagePath = "/search/:str";

export function useNavigateSearch() {
    const navigate = useNavigate();
    const navigateSearch = useCallback((keyword: string) => {
        navigate(`/search/${encodeURIComponent(keyword.trim())}`);
    }, [navigate]);
    return { navigateSearch };
}

export enum SearchTab {
    video = "video",
    user = "user"
}

const CATEGORY_LABEL: Record<SearchTab, string> = {
    [SearchTab.video]: "视频",
    [SearchTab.user]: "用户"
};

const SearchPage: React.FC = () => {
    const { str } = useParams<{ str: string }>();
    // 保证变量只声明一次
    const [searchInput, setSearchInput] = useState(str || "");
    const [activeTab, setActiveTab] = useState<SearchTab>(SearchTab.video);
    const { navigateSearch } = useNavigateSearch();

    const handleSearch = () => {
        navigateSearch(searchInput);
    };

    const handleInputKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter") handleSearch();
    };

    return (
        <div className="search-page-container">
            <Header hideSearch={true} />
            <div className="search-bar-wrapper">
                <div className="search-bar">
                    <input
                        type="text"
                        value={searchInput}
                        onChange={e => setSearchInput(e.target.value)}
                        onKeyDown={handleInputKeyDown}
                        placeholder="搜索视频、UP主"
                    />
                    <button onClick={handleSearch}>搜索</button>
                </div>
            </div>
            <div className="search-category-tabs">
                {Object.values(SearchTab).map(tab => (
                    <div
                        key={tab}
                        className={`search-category-tab ${activeTab === tab ? "active" : ""}`}
                        onClick={() => setActiveTab(tab)}
                    >
                        <div className="search-category-content">
                            {CATEGORY_LABEL[tab]}
                        </div>
                    </div>
                ))}
            </div>
            <div className="search-results">
                {activeTab === SearchTab.video ? (
                    <SearchVideosTab keyword={str} />
                ) : (
                    <SearchUsersTab keyword={str} />
                )}
            </div>
        </div>
    );
};

export default SearchPage;
