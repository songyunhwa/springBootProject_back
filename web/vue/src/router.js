import Home from "@/components/youtube/Home";
import Login from './components/user/Login'
import Logout from "@/components/user/Logout";
import WishedHome from "@/components/wished/WishedHome";
import MyListHome from "@/components/myList/MyListHome";
import Password from "@/components/user/Password";
import { createWebHistory, createRouter } from "vue-router";

const routes = [
    {
        path: "/",
        component: Home,
    },
    {
        path: "/login",
        component: Login,
    },
    {
        path: "/wished",
        component: WishedHome,
    },
    {
        path: "/myList",
        component: MyListHome,
    },
    {
        path: "/logout",
        component: Logout,
    },
    {
        path: "/password",
        component: Password,
    }
];


const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;