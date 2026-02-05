import { commons } from '/js/commons.js';
import { bindCommons } from '/js/commons.js';
import { bindMain } from '/js/main.js';
import { bindAuth } from '/js/account/auth.js';
import { bindUser } from '/js/account/user.js';
import { bindAlarm } from '/js/account/alarm.js';
import { bindBoard } from '/js/board/board.js';
import { bindPost } from '/js/board/post.js';
import { bindComment } from '/js/board/comment.js';
import { bindDashboard } from "/js/admin/dashboard.js";
import { bindUserList } from "/js/admin/userList.js";
import { bindDeletedPost } from "/js/admin/deletedPost.js";
import { bindDeletedComment } from "/js/admin/deletedComment.js";

// 바인딩
document.addEventListener('DOMContentLoaded', () =>
{
    commons.applyTheme();
    bindCommons(commons);
    bindMain(commons);
    bindAuth(commons);
    bindUser(commons);
    bindAlarm(commons);
    bindBoard(commons);
    bindPost(commons);
    bindComment(commons);
    bindDashboard(commons);
    bindUserList(commons);
    bindDeletedPost(commons);
    bindDeletedComment(commons);
});