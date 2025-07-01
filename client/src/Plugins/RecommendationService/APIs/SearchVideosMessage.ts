/**
 * SearchVideosMessage
 * desc: 提供模糊搜索功能，返回匹配的视频列表。
 * @param searchString: String (搜索关键字，用于匹配视频标题。)
 * @param rangeL: Int (分页查询的起始位置。)
 * @param rangeR: Int (分页查询的结束位置。)
 * @return searchResult: Video[] (返回的视频列表，包含匹配的所有视频信息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class SearchVideosMessage extends TongWenMessage {
    constructor(
        public  searchString: string,
        public  rangeL: number,
        public  rangeR: number
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10011"
    }
}

