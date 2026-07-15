<template>
  <div class="dashboard-page">
    <section class="dashboard-heading">
      <div>
        <p class="eyebrow">MEDICAL OPERATIONS</p>
        <h1>医疗数据概览</h1>
        <p class="subtitle">集中查看药品、医生、企业及销售地点的实时数据。</p>
      </div>
      <div class="status-chip" :class="{ degraded: dashboardDegraded }">
        <i :class="dashboardDegraded ? 'el-icon-warning-outline' : 'el-icon-success'"></i>
        {{ dashboardStatusText }}
      </div>
    </section>

    <section class="metric-grid" v-loading="dashboardLoading">
      <article v-for="metric in metrics" :key="metric.key" class="metric-card">
        <div class="metric-icon" :style="{ background: metric.color }">
          <i :class="metric.icon"></i>
        </div>
        <div>
          <p>{{ metric.label }}</p>
          <strong>{{ metric.value }}</strong>
          <span>{{ metric.unit }}</span>
        </div>
      </article>
    </section>

    <section class="charts-grid">
      <el-card class="chart-card" shadow="never">
        <div slot="header" class="section-title">
          <span>医生职称分布</span>
          <small>按职称级别统计</small>
        </div>
        <div ref="doctorLevelChart" class="chart-canvas"></div>
      </el-card>
      <el-card class="chart-card" shadow="never">
        <div slot="header" class="section-title">
          <span>医院科室构成</span>
          <small>按治疗类型统计</small>
        </div>
        <div ref="treatTypeChart" class="chart-canvas"></div>
      </el-card>
    </section>

    <section class="content-grid">
      <el-card class="carousel-card" shadow="never">
        <div slot="header" class="section-title">
          <span>医疗资讯</span>
          <small>健康服务与用药管理</small>
        </div>
        <el-carousel :interval="4000" height="300px" trigger="click" arrow="hover">
          <el-carousel-item v-for="(item, index) in picList" :key="index">
            <el-image
              :src="item.image"
              :alt="item.alt"
              fit="cover"
              class="carousel-image"
            ></el-image>
          </el-carousel-item>
        </el-carousel>
      </el-card>

      <el-card class="quick-card" shadow="never">
        <div slot="header" class="section-title">
          <span>功能导航</span>
          <small>常用业务入口</small>
        </div>
        <div class="quick-list">
          <div
            v-for="card in availableCardList"
            :key="card.content"
            class="quick-item"
            role="button"
            tabindex="0"
            @click="goTo(card.route)"
            @keydown.enter="goTo(card.route)"
          >
            <el-image
              :src="card.pic"
              :alt="card.content"
              fit="cover"
              class="quick-image"
            />
            <span>{{ card.content }}</span>
          </div>
        </div>
      </el-card>
    </section>

    <section class="news-section">
      <el-card class="news-card" shadow="never">
        <div slot="header" class="section-title">
          <span>最新政策资讯</span>
          <small>医疗政策 / 企业政策 / 必备材料</small>
        </div>
        <ul class="news-list" v-loading="dashboardLoading">
          <li
            v-for="item in newsList"
            :key="item.id"
            class="news-item"
          >
            <span class="news-tag" :class="newsTagClass(item.id)">{{ newsTagText(item.id) }}</span>
            <div class="news-body">
              <p class="news-title">{{ item.title }}</p>
              <p class="news-summary">{{ item.summary }}</p>
            </div>
            <span class="news-time">{{ formatTime(item.publishedAt) }}</span>
          </li>
          <li v-if="!newsList.length && !dashboardLoading" class="news-empty">
            暂无资讯
          </li>
        </ul>
      </el-card>
    </section>
  </div>
</template>

<script>
import { getDashboard } from '../../api/dashboard';
import * as echarts from 'echarts';
// Vite 用 ESM，浏览器没有 require。图片资源必须改为静态 import，
// 否则组件 data() 执行时抛 ReferenceError: require is not defined。
import healthcareTeam from '../../assets/medical-samples/healthcare-team.jpg';
import medicalLab from '../../assets/medical-samples/medical-lab.jpg';
import stethoscope from '../../assets/medical-samples/stethoscope.jpg';
import medicineBlister from '../../assets/medical-samples/medicine-blister.jpg';
import medicineCapsule from '../../assets/medical-samples/medicine-capsule.jpg';
import medicineHand from '../../assets/medical-samples/medicine-hand.jpg';
import medicineAssorted from '../../assets/medical-samples/medicine-assorted.jpg';
import { allowedPath } from '../../utils/permissions';

const metricDefinitions = [
  {
    key: 'drugCount',
    aliases: ['drugCount', 'drugTotal', 'totalDrugs', 'drugs'],
    label: '药品总数',
    unit: '种',
    icon: 'el-icon-first-aid-kit',
    color: 'linear-gradient(135deg, #21b7a8, #45d3ba)',
  },
  {
    key: 'doctorCount',
    aliases: ['doctorCount', 'doctorTotal', 'totalDoctors', 'doctors'],
    label: '医生总数',
    unit: '人',
    icon: 'el-icon-user',
    color: 'linear-gradient(135deg, #3f8efc, #69b7ff)',
  },
  {
    key: 'saleCount',
    aliases: ['saleCount', 'salesCount', 'saleTotal', 'totalSales', 'sales'],
    label: '销售地点',
    unit: '家',
    icon: 'el-icon-location-outline',
    color: 'linear-gradient(135deg, #f59d32, #ffc45d)',
  },
  {
    key: 'companyCount',
    aliases: ['companyCount', 'companyTotal', 'totalCompanies', 'companies'],
    label: '医药企业',
    unit: '家',
    icon: 'el-icon-office-building',
    color: 'linear-gradient(135deg, #7a66ee, #a58cf5)',
  },
];

export default {
  name: 'welcome',
  data() {
    return {
      dashboardLoading: false,
      dashboardDegraded: false,
      dashboardData: {},
      doctorLevelChart: null,
      treatTypeChart: null,
      picList: [
        {
          image: healthcareTeam,
          alt: '医疗团队协作示例图',
        },
        {
          image: medicalLab,
          alt: '医学检验实验室示例图',
        },
        {
          image: stethoscope,
          alt: '听诊器与医疗器械示例图',
        },
        {
          image: medicineBlister,
          alt: '药品板装示例图',
        },
        {
          image: medicineCapsule,
          alt: '胶囊药品示例图',
        },
      ],
      cardList: [
        {
          pic: stethoscope,
          content: '基础信息管理',
          route: '/base/city',
        },
        {
          pic: medicineBlister,
          content: '药品信息管理',
          route: '/manage/drug',
        },
        {
          pic: healthcareTeam,
          content: '医保政策管理',
          route: '/manage/medical/policy',
        },
        {
          pic: medicalLab,
          content: '企业政策管理',
          route: '/manage/company/policy',
        },
        {
          pic: medicineHand,
          content: '医生信息管理',
          route: '/manage/doctor',
        },
        {
          pic: medicineAssorted,
          content: '必备材料管理',
          route: '/manage/material',
        },
      ],
    };
  },
  computed: {
    dashboardStatusText() {
      return this.dashboardDegraded ? '统计接口暂不可用，业务入口仍可使用' : '数据已同步';
    },
    metrics() {
      return metricDefinitions.map((definition) => ({
        ...definition,
        value: this.pickMetric(definition.aliases),
      }));
    },
    newsList() {
      return (this.dashboardData.news || []).slice(0, 5);
    },
    availableCardList() {
      const appState = this.$store && this.$store.state && this.$store.state.app;
      const allowed = appState && appState.allowedRoutePaths;
      if (!Array.isArray(allowed) || !allowed.length) {
        return this.cardList;
      }
      return this.cardList.filter((card) => allowedPath(card.route, allowed));
    },
  },
  mounted() {
    this.loadDashboard();
    window.addEventListener('resize', this.handleResize);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize);
    if (this.doctorLevelChart) {
      this.doctorLevelChart.dispose();
      this.doctorLevelChart = null;
    }
    if (this.treatTypeChart) {
      this.treatTypeChart.dispose();
      this.treatTypeChart = null;
    }
  },
  methods: {
    handleResize() {
      if (this.doctorLevelChart) {
        this.doctorLevelChart.resize();
      }
      if (this.treatTypeChart) {
        this.treatTypeChart.resize();
      }
    },
    renderCharts() {
      this.renderDoctorLevelChart();
      this.renderTreatTypeChart();
    },
    renderDoctorLevelChart() {
      if (!this.$refs.doctorLevelChart) {
        return;
      }
      if (!this.doctorLevelChart) {
        this.doctorLevelChart = echarts.init(this.$refs.doctorLevelChart);
      }
      const levels = this.dashboardData.doctorLevels || [];
      this.doctorLevelChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: levels.map((item) => item.name),
          axisLabel: { interval: 0, rotate: levels.length > 4 ? 20 : 0 },
        },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          {
            name: '医生人数',
            type: 'bar',
            data: levels.map((item) => item.value),
            barMaxWidth: 36,
            itemStyle: {
              borderRadius: [6, 6, 0, 0],
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [
                  { offset: 0, color: '#69b7ff' },
                  { offset: 1, color: '#3f8efc' },
                ],
              },
            },
          },
        ],
      });
    },
    renderTreatTypeChart() {
      if (!this.$refs.treatTypeChart) {
        return;
      }
      if (!this.treatTypeChart) {
        this.treatTypeChart = echarts.init(this.$refs.treatTypeChart);
      }
      const types = this.dashboardData.treatTypes || [];
      this.treatTypeChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, type: 'scroll' },
        series: [
          {
            name: '治疗类型',
            type: 'pie',
            radius: ['38%', '62%'],
            center: ['50%', '46%'],
            avoidLabelOverlap: true,
            itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
            label: { show: true, formatter: '{b}\n{d}%' },
            data: types.map((item) => ({ name: item.name, value: item.value })),
          },
        ],
      });
    },
    goTo(route) {
      if (!route) {
        return;
      }
      const appState = this.$store && this.$store.state && this.$store.state.app;
      const allowed = appState && appState.allowedRoutePaths;
      if (Array.isArray(allowed) && allowed.length && !allowedPath(route, allowed)) {
        return;
      }
      // 仅在路由不同时跳转，避免重复点击产生多余的 history 记录。
      if (this.$route.path !== route) {
        this.$router.push(route).catch(() => {
          // 目标路由已在动态菜单中注册；忽略重复导航等非致命错误。
        });
      }
    },
    formatTime(value) {
      if (!value) {
        return '';
      }
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return value;
      }
      const pad = (n) => String(n).padStart(2, '0');
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
    },
    newsTagText(id) {
      if (!id) return '资讯';
      if (id.startsWith('medical-')) return '医疗政策';
      if (id.startsWith('company-')) return '企业政策';
      if (id.startsWith('material-')) return '必备材料';
      return '资讯';
    },
    newsTagClass(id) {
      if (!id) return 'tag-default';
      if (id.startsWith('medical-')) return 'tag-medical';
      if (id.startsWith('company-')) return 'tag-company';
      if (id.startsWith('material-')) return 'tag-material';
      return 'tag-default';
    },
    pickMetric(aliases) {
      const sources = [
        this.dashboardData,
        this.dashboardData.counts,
        this.dashboardData.statistics,
        this.dashboardData.summary,
      ].filter(Boolean);
      for (const source of sources) {
        for (const key of aliases) {
          if (source[key] !== undefined && source[key] !== null) {
            return source[key];
          }
        }
      }
      return '--';
    },
    async loadDashboard() {
      this.dashboardLoading = true;
      try {
        const res = await getDashboard();
        if (!res.data || Number(res.data.code) !== 20000) {
          throw new Error('统计数据加载失败');
        }
        this.dashboardData = res.data.data || {};
        this.dashboardDegraded = false;
        this.$nextTick(() => this.renderCharts());
      } catch (error) {
        // 仪表盘是增强功能，失败不阻塞首页和其他业务模块。
        this.dashboardData = {};
        this.dashboardDegraded = true;
      } finally {
        this.dashboardLoading = false;
      }
    },
  },
};
</script>

<style lang="less" scoped>
.dashboard-page {
  min-height: 100%;
  padding: 28px;
  box-sizing: border-box;
  color: #263b3a;
  background: linear-gradient(160deg, #f5fbfa 0%, #f5f8fb 65%, #eef8f6 100%);
  overflow-y: auto;
}
.dashboard-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 22px;
}
.eyebrow {
  margin: 0 0 5px;
  color: #20a99d;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1.8px;
}
h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
}
.subtitle {
  margin: 8px 0 0;
  color: #718281;
}
.status-chip {
  margin-top: 7px;
  padding: 9px 14px;
  border: 1px solid #c8ebe5;
  border-radius: 20px;
  color: #168f84;
  background: #edfaf7;
  font-size: 13px;
}
.status-chip.degraded {
  color: #a16a19;
  border-color: #f1dab7;
  background: #fff8ed;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(170px, 1fr));
  gap: 16px;
  min-height: 105px;
}
.metric-card {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 20px;
  border: 1px solid #e5eeee;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8px 22px rgba(41, 91, 87, 0.07);
}
.metric-icon {
  display: grid;
  width: 50px;
  height: 50px;
  flex: 0 0 50px;
  place-items: center;
  border-radius: 13px;
  color: #fff;
  font-size: 24px;
}
.metric-card p {
  margin: 0 0 4px;
  color: #718281;
  font-size: 13px;
}
.metric-card strong {
  font-size: 27px;
  line-height: 1;
}
.metric-card span {
  margin-left: 5px;
  color: #93a09f;
  font-size: 12px;
}
.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin-top: 20px;
}
.chart-card {
  border: 0;
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(41, 91, 87, 0.07);
}
.chart-canvas {
  width: 100%;
  height: 300px;
}
.content-grid {
  display: grid;
  grid-template-columns: minmax(480px, 1.45fr) minmax(330px, 1fr);
  gap: 18px;
  margin-top: 20px;
}
.carousel-card,
.quick-card {
  border: 0;
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(41, 91, 87, 0.07);
}
.section-title {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-weight: 700;
}
.section-title small {
  color: #8a9998;
  font-weight: 400;
}
.carousel-image {
  width: 100%;
  height: 300px;
  border-radius: 8px;
}
.quick-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.quick-item {
  position: relative;
  height: 86px;
  overflow: hidden;
  border-radius: 9px;
  background: #edf4f3;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.quick-item:hover,
.quick-item:focus-visible {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(41, 91, 87, 0.18);
  outline: none;
}
.quick-image {
  width: 100%;
  height: 100%;
  opacity: 0.72;
}
.quick-item span {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 7px 9px;
  color: #fff;
  background: linear-gradient(transparent, rgba(20, 48, 46, 0.82));
  font-size: 13px;
  font-weight: 600;
}
.news-section {
  margin-top: 20px;
}
.news-card {
  border: 0;
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(41, 91, 87, 0.07);
}
.news-list {
  margin: 0;
  padding: 0;
  list-style: none;
}
.news-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 4px;
  border-bottom: 1px solid #eef3f3;
}
.news-item:last-child {
  border-bottom: 0;
}
.news-tag {
  flex: 0 0 auto;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}
.news-tag.tag-medical {
  color: #168f84;
  background: #e3f6f2;
}
.news-tag.tag-company {
  color: #5b4dc4;
  background: #ece9fb;
}
.news-tag.tag-material {
  color: #b5641a;
  background: #fdf0df;
}
.news-tag.tag-default {
  color: #5a6b6a;
  background: #eef3f3;
}
.news-body {
  flex: 1 1 auto;
  min-width: 0;
}
.news-title {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #263b3a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.news-summary {
  margin: 0;
  font-size: 12px;
  color: #8a9998;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.news-time {
  flex: 0 0 auto;
  color: #93a09f;
  font-size: 12px;
  white-space: nowrap;
}
.news-empty {
  padding: 24px;
  text-align: center;
  color: #93a09f;
  font-size: 13px;
}
@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(170px, 1fr));
  }
  .charts-grid {
    grid-template-columns: 1fr;
  }
  .content-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 720px) {
  .dashboard-page {
    padding: 18px;
  }
  .dashboard-heading {
    flex-direction: column;
  }
  .metric-grid {
    grid-template-columns: 1fr;
  }
  .content-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
