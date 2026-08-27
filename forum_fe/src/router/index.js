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
		path: '/',
		component: () =>
			import(
				 '../pages/home/Home.vue'
			),
		beforeEnter: handleBeforeEnter
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
		path: '/post/:id',
		component: () =>
			import(
				"@/components/Post/PostDetail.vue"
			),
		beforeEnter: handleBeforeEnter
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