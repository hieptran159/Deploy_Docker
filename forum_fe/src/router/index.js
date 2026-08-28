import { createRouter, createWebHistory } from 'vue-router';

import {LOCALKEYS , getItemLocal} from '../storages/localStorage'

const handleBeforeEnter = (to, form, next) => {

	if (getItemLocal(LOCALKEYS.ACCESS_TOKEN)!=null && getItemLocal(LOCALKEYS.USER_ID)!=null) {
		next();
	} else {
		next('/login');
	}

};

// Chỉ chặn khi đã biết chắc không phải admin; nếu chưa rõ thì để trang tự kiểm tra
const handleAdminEnter = (to, form, next) => {
	if (getItemLocal(LOCALKEYS.ACCESS_TOKEN) == null || getItemLocal(LOCALKEYS.USER_ID) == null) {
		next('/login');
	} else if (getItemLocal(LOCALKEYS.IS_ADMIN) === false) {
		next('/');
	} else {
		next();
	}
};

const routers = [
	{
		// Trang chủ xem được không cần đăng nhập (khách chỉ xem, muốn tương tác thì mới cần tài khoản)
		path: '/',
		component: () =>
			import(
				 '../pages/home/Home.vue'
			),
	},

    {
		path: '/login',
		component: () =>
			import(
				 '../pages/auth/Login.vue'
			),
	},

	{
		path: '/signup',
		component: () =>
			import(
				 '../pages/auth/SignUp.vue'
			),
	},

	{
		path: '/forgot-password',
		component: () =>
			import(
				 '../pages/auth/ForgotPassword.vue'
			),
	},

	{
		// Khách xem được chi tiết bài (đọc bài + bình luận), chỉ không thao tác được
		path: '/post/:id',
		component: () =>
			import(
				"@/components/Post/PostDetail.vue"
			),
	},

	{
		// Xem bài theo hashtag — khách cũng xem được (như trang chủ)
		path: '/tag/:tag',
		component: () =>
			import(
				"@/pages/tag/TagPage.vue"
			),
	},

	{
		path: '/follow',
		component: () =>
			import(
				"@/pages/Follow/FollowVue.vue"
			),
		beforeEnter: handleBeforeEnter
	},

	{
		path: '/chat',
		component: () =>
			import(
				"@/pages/chat/Chat.vue"
			),
		beforeEnter: handleBeforeEnter
	},

	{
		path: '/users',
		component: () =>
			import(
				"@/pages/user/SearchUser.vue"
			),
		beforeEnter: handleBeforeEnter
	},

	{
		path: '/profile/edit',
		component: () =>
			import(
				"@/pages/profile/EditProfile.vue"
			),
		beforeEnter: handleBeforeEnter
	},

	{
		path: '/user/:id',
		component: () =>
			import(
				"@/pages/user/UserProfile.vue"
			),
		beforeEnter: handleBeforeEnter
	},

	{
		path: '/saved',
		component: () =>
			import(
				"@/pages/Saved.vue"
			),
		beforeEnter: handleBeforeEnter
	},

	{
		path: '/notifications',
		component: () =>
			import(
				"@/pages/Notifications.vue"
			),
		beforeEnter: handleBeforeEnter
	},

	{
		path: '/drafts',
		component: () =>
			import(
				"@/pages/Drafts.vue"
			),
		beforeEnter: handleBeforeEnter
	},

	{
		path: '/admin',
		component: () =>
			import(
				"@/pages/admin/AdminPage.vue"
			),
		beforeEnter: handleAdminEnter
	},

]

const vueRouter = createRouter({
	history: createWebHistory(),
	routes: routers,
});

export default vueRouter;