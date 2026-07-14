<template>
  <div>
    <el-menu
      class="el-menu-vertical-demo"
      background-color="#2abeb2"
      text-color="#000000"
      active-text-color="#ffffff"
      router
      :default-active="$route.path"
    >
      <div class="MenuBackground">
        <fragment v-for="(item, index) in submenuList" :key="index">
          <!-- 一级菜单（没有任何子级菜单）-->
          <el-menu-item :index="item.path" v-if="!item.children || !item.children.length">
            <i :class="item.icon"></i>
            <span>
            {{ item.title }}
          </span>
          </el-menu-item>
          <!-- 一级菜单（有子级菜单）-->
          <el-submenu :index="item.path" v-else>
            <template slot="title">
              <i :class="item.icon"></i>
              <span>{{ item.title }}</span>
            </template>
            <!-- 遍历二级菜单容器 -->
            <fragment v-for="(i, index) in item.children" :key="index">
              <!-- 判断二级菜单（没有三级菜单）-->
              <el-menu-item :index="i.path" v-if="!i.children || !i.children.length">
                <i :class="i.icon"></i>
                <span>{{ i.title }}</span>
              </el-menu-item>
              <!-- 判断二级菜单（有三级菜单）-->
              <el-submenu :index="i.path" v-else>
                <template slot="title">
                  <i :class="i.icon"></i>
                  <span>{{ i.title }}</span>
                </template>
                <el-menu-item
                        :index="j.path"
                        v-for="(j, index) in i.children"
                        :key="index"
                >
                  <i :class="j.icon"></i>
                  <span>{{ j.title }}</span>
                </el-menu-item>
              </el-submenu>
            </fragment>
          </el-submenu>
        </fragment>
      </div>
    </el-menu>
  </div>
</template>
<script>
export default {
  name: 'PageSider',
  data() {
    return {
      submenuList: [],
    };
  },
  methods: {
    handleMenuListData(data, arr) {
      (Array.isArray(data) ? data : []).forEach((route) => {
        if (!route || !route.path || route.path === '/user/login') {
          return;
        }
        arr.push({
          path: route.path,
          title: (route.meta && route.meta.title) || route.name || '未命名菜单',
          icon: 'el-icon-menu',
          children: this.handleMenuListData(route.children, []),
        });
      });
      return arr;
    },
    refreshMenuList(routes) {
      const availableRoutes = (Array.isArray(routes) ? routes : []).filter(
        (route) => route && route.path !== '/user/login' && !route.redirect
      );
      const layoutRoute = availableRoutes.find(
        (route) => Array.isArray(route.children) && route.children.length
      );
      const menuRoutes = layoutRoute ? layoutRoute.children : availableRoutes;
      this.submenuList = this.handleMenuListData(menuRoutes, []);
    },
  },
  computed: {
    fullMenuList() {
      return this.$store.getters.menuList || [];
    },
  },
  watch: {
    fullMenuList: {
      immediate: true,
      deep: true,
      handler(routes) {
        this.refreshMenuList(routes);
      },
    },
  },
};
</script>
<style lang="less" scoped>
.el-menu-vertical-demo:not(.el-menu--collapse) {
  width: 200px;
  min-height: 400px;
}
.medicine_system_title {
  width: 100%;
  color: #fff;
  font-size: 20px;
  height: 145px;
  line-height: 145px;
  text-align: center;
  background-color: #233646;
  cursor: default;
}
.el-menu {
  height: 100%;
  border-right: 0;
  }
.MenuBackground{
  background: url("../../../assets/MenuBackGround.jpg");
  background-size: 120%;
  min-height: calc(100vh - 100px);
  }
</style>
