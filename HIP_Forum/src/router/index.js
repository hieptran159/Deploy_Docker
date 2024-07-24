import { createRouter, createWebHistory } from 'vue-router';

import {LOCALKEYS , getItemLocal} from '../storages/localStorage'

const handleBeforeEnter = (to, form, next) => {

	if (getItemLocal(LOCALKEYS.ACCESS_TOKEN)!=null && getItemLocal(LOCALKEYS.USER_ID)!=null) {
		next();
	} else {
		next('/login');
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

]

const vueRouter = createRouter({
	history: createWebHistory(),
	routes: routers,
});

export default vueRouter;