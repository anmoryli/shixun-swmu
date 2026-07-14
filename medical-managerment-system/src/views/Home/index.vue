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
          <div v-for="card in cardList" :key="card.content" class="quick-item">
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
  </div>
</template>

<script>
import { getDashboard } from '../../api/dashboard';

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
      picList: [
        {
          image: require('../../assets/medical-samples/healthcare-team.jpg'),
          alt: '医疗团队协作示例图',
        },
        {
          image: require('../../assets/medical-samples/medical-lab.jpg'),
          alt: '医学检验实验室示例图',
        },
        {
          image: require('../../assets/medical-samples/stethoscope.jpg'),
          alt: '听诊器与医疗器械示例图',
        },
        {
          image: require('../../assets/medical-samples/medicine-blister.jpg'),
          alt: '药品板装示例图',
        },
        {
          image: require('../../assets/medical-samples/medicine-capsule.jpg'),
          alt: '胶囊药品示例图',
        },
      ],
      cardList: [
        {
          pic: require('../../assets/medical-samples/stethoscope.jpg'),
          content: '基础信息管理',
        },
        {
          pic: require('../../assets/medical-samples/medicine-blister.jpg'),
          content: '药品信息管理',
        },
        {
          pic: require('../../assets/medical-samples/healthcare-team.jpg'),
          content: '医保政策管理',
        },
        {
          pic: require('../../assets/medical-samples/medical-lab.jpg'),
          content: '企业政策管理',
        },
        {
          pic: require('../../assets/medical-samples/medicine-hand.jpg'),
          content: '医生信息管理',
        },
        {
          pic: require('../../assets/medical-samples/medicine-assorted.jpg'),
          content: '必备材料管理',
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
  },
  mounted() {
    this.loadDashboard();
  },
  methods: {
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
@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(170px, 1fr));
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
