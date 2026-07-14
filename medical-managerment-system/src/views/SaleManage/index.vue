<template>
  <el-container>
    <!-- 头部区域 -->
    <el-header height="76px">
      <h2>销售地点管理</h2>
      <!-- 面包屑导航区域 -->
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>销售地点管理</el-breadcrumb-item>
      </el-breadcrumb>
    </el-header>
    <el-main>
      <el-switch
        v-model="visualization"
        active-text="销售地点地图展示"
        class="map-switch"
        style="--el-switch-on-color: #13ce66"
        @change="handleVisualizationChange"
      />
      <!-- 列表视图 -->
      <div v-show="!visualization">
        <div class="main-title">
          <h3>销售地点列表</h3>
          <button class="new-add" @click="addFormVisible = true" v-if="hasRole" />
        </div>
        <!-- 搜索 -->
        <el-row :gutter="20">
          <el-col :span="23" class="search-col">
            <keep-alive>
              <el-input
                placeholder="查询（输入要查询的药店名称）"
                size="small"
                v-model="keyword"
                @input="handelQuery"
              >
              </el-input>
            </keep-alive>
          </el-col>
        </el-row>
        <!-- 表格 -->
        <el-table
          stripe
          :default-sort="{ prop: 'date', order: 'descending' }"
          :data="tableData.list"
          highlight-current-row
        >
          <el-table-column prop="saleId" label="药店编号" sortable />
          <el-table-column prop="saleName" label="药店名称" />
          <el-table-column prop="salePhone" label="药店电话" />
          <el-table-column prop="address" label="详细地址" min-width="190">
            <template #default="scope">
              <span>{{ scope.row.address || "暂无地址" }}</span>
            </template>
          </el-table-column>
          <el-table-column label="地理坐标" min-width="190">
            <template #default="scope">
              <div v-if="hasCoordinate(scope.row)" class="coordinate-badge">
                <i class="el-icon-location-outline"></i>
                {{ formatCoordinate(scope.row) }}
              </div>
              <span v-else class="coordinate-empty">暂未标注</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" v-if="hasRole">
            <!-- 通过slot-scope拿到对应行的数据 -->
            <template #default="scope">
              <button
                class="table-btn-delete"
                @click="
                  handleDeleteSalePlace(scope.row.saleId, scope.row.saleName)
                "
              ></button>
              <button
                class="table-btn-update"
                @click="
                  handleModifyFormVisible(scope.row)
                "
              />
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <pagination
            :page="currentPage"
            :layout="'total,prev,pager,next,jumper'"
            :total="tableData.total"
            :page-size="pageSize"
            @currentChange="handleCurrentChange($event)"
            @update:page="currentPage = $event"
          ></pagination>
        </div>
      </div>
      <!-- 地图视图 -->
      <div v-show="visualization" class="map-panel">
        <div class="map-toolbar">
          <el-button
            type="primary"
            v-if="hasRole"
            @click="handleAdd"
          >新增地点</el-button>
          <span v-if="addStatus === 1" class="map-hint">
            <i class="el-icon-location-outline"></i>
            请在地图上点击要新增的位置
          </span>
        </div>
        <div id="mapContainer" class="map-container"></div>
        <div v-if="!amapJsKey" class="map-placeholder">
          未配置高德地图 Key，请在 .env 设置 VITE_AMAP_JS_KEY 后重新构建
        </div>
      </div>
    </el-main>
    <el-dialog
      title="新增销售地点"
      v-model="addFormVisible"
      :modal-append-to-body="false"
      @close="handleAddClose"
    >
      <el-form
        :model="addForm"
        hide-required-asterisk
        ref="addForm"
        label-width="110px"
      >
        <el-form-item label="药店名称" prop="saleName" :rules="rules.nameRules">
          <el-input
            v-model.trim="addForm.saleName"
            autocomplete="off"
            required
          ></el-input>
        </el-form-item>
        <el-form-item
          label="药店电话"
          prop="salePhone"
          :rules="rules.phoneRules"
        >
          <el-input
            v-model.number="addForm.salePhone"
            autocomplete="off"
            required
          ></el-input>
        </el-form-item>
        <el-form-item label="详细地址" prop="address" :rules="saleRules.address">
          <el-input
            v-model.trim="addForm.address"
            autocomplete="off"
            placeholder="地图新增时自动填充，可手动修正"
          ></el-input>
        </el-form-item>
        <el-form-item label="经度" prop="longitude" :rules="saleRules.longitude">
          <el-input-number
            v-model="addForm.longitude"
            :min="-180"
            :max="180"
            :precision="6"
            :step="0.000001"
            controls-position="right"
          ></el-input-number>
          <span class="coordinate-hint">范围 -180 ～ 180</span>
        </el-form-item>
        <el-form-item label="纬度" prop="latitude" :rules="saleRules.latitude">
          <el-input-number
            v-model="addForm.latitude"
            :min="-90"
            :max="90"
            :precision="6"
            :step="0.000001"
            controls-position="right"
          ></el-input-number>
          <span class="coordinate-hint">范围 -90 ～ 90</span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="addFormVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleAddSalePlace('addForm')"
          >确 定</el-button
        >
      </div>
    </el-dialog>
    <!-- 点击修改后的弹窗 -->
    <el-dialog
      title="修改销售地点信息"
      v-model="modifyFormVisible"
      :modal-append-to-body="false"
      @close="handleModifyClose"
    >
      <el-form
        :model="modifyForm"
        hide-required-asterisk
        ref="modifyForm"
        label-width="110px"
      >
        <el-form-item label="药店编号">
          <el-input
            v-model="modifyForm.saleId"
            autocomplete="off"
            disabled
          ></el-input>
        </el-form-item>
        <el-form-item label="药店名称" prop="saleName" :rules="rules.nameRules">
          <el-input
            v-model.trim="modifyForm.saleName"
            autocomplete="off"
            required
          ></el-input>
        </el-form-item>
        <el-form-item
          label="药店电话"
          prop="salePhone"
          :rules="rules.phoneRules"
        >
          <el-input
            v-model.number="modifyForm.salePhone"
            autocomplete="off"
            required
          ></el-input>
        </el-form-item>
        <el-form-item label="详细地址" prop="address" :rules="saleRules.address">
          <el-input
            v-model.trim="modifyForm.address"
            autocomplete="off"
            placeholder="请输入药店所在的详细地址"
          ></el-input>
        </el-form-item>
        <el-form-item label="经度" prop="longitude" :rules="saleRules.longitude">
          <el-input-number
            v-model="modifyForm.longitude"
            :min="-180"
            :max="180"
            :precision="6"
            :step="0.000001"
            controls-position="right"
          ></el-input-number>
          <span class="coordinate-hint">范围 -180 ～ 180</span>
        </el-form-item>
        <el-form-item label="纬度" prop="latitude" :rules="saleRules.latitude">
          <el-input-number
            v-model="modifyForm.latitude"
            :min="-90"
            :max="90"
            :precision="6"
            :step="0.000001"
            controls-position="right"
          ></el-input-number>
          <span class="coordinate-hint">范围 -90 ～ 90</span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="modifyFormVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleModifySalePlace('modifyForm')"
          >确 定</el-button
        >
      </div>
    </el-dialog>
  </el-container>
</template>

<script>
import Pagination from '../../components/Pagination.vue';
import { mapGetters } from 'vuex';
import rules from '../../utils/validator';
import AMapLoader from '@amap/amap-jsapi-loader';
import axios from 'axios';

function coordinateValidator(min, max, label) {
  return (rule, value, callback) => {
    if (value === '' || value === null || value === undefined) {
      callback(new Error(`${label}不能为空`));
      return;
    }
    const coordinate = Number(value);
    if (!Number.isFinite(coordinate) || coordinate < min || coordinate > max) {
      callback(new Error(`${label}范围应为 ${min} 到 ${max}`));
      return;
    }
    callback();
  };
}

const emptySaleForm = () => ({
  saleName: '',
  salePhone: '',
  address: '',
  longitude: null,
  latitude: null,
});

export default {
  name: 'SaleManage',
  components: {
    Pagination,
  },
  data() {
    return {
      currentPage: 1,
      pageSize: 5, // 每页的数据条数
      keywordDefault: '',
      addFormVisible: false, // 控制新增销售地点页面的显示
      addForm: emptySaleForm(),
      modifyFormVisible: false, // 控制修改信息页面的显示
      modifyForm: {
        saleId: '',
        saleName: '',
        salePhone: '',
        address: '',
        longitude: null,
        latitude: null,
      },
      rules, // 封装好的表单验证
      saleRules: {
        address: [
          { required: true, message: '详细地址不能为空', trigger: 'blur' },
          { min: 4, max: 200, message: '详细地址长度应为 4 到 200 个字符', trigger: 'blur' },
        ],
        longitude: [
          { validator: coordinateValidator(-180, 180, '经度'), trigger: 'change' },
        ],
        latitude: [
          { validator: coordinateValidator(-90, 90, '纬度'), trigger: 'change' },
        ],
      },
      // 地图相关
      visualization: false, // 列表/地图视图切换
      map: null, // 高德地图实例
      amap: null, // AMap 命名空间（用于 new AMap.Marker 等）
      markers: [], // 当前地图上的标记点
      addStatus: 0, // 1=等待用户在地图上点击新增位置
    };
  },
  methods: {
    // 切换分页及首次进入获取数据
    getSalePlaceInfo() {
      this.$store.dispatch('saleInfoManage/getSalePlaceInfo', {
        pn: this.currentPage,
        size: this.pageSize,
      });
    },
    // 拉取全部销售地点（地图标记点用）
    getAllSalePlaceInfo() {
      return this.$store.dispatch('saleInfoManage/getAllSalePlaceInfo');
    },
    // 当前页改变时触发,跳转其他页
    handleCurrentChange(event) {
      this.currentPage = event.page;
      if (this.keyword.length) {
        this.handelQuery(this.keyword);
      } else {
        this.getSalePlaceInfo();
      }
    },
    // 通过关键字查询数据
    handelQuery(keyword) {
      this.$store.dispatch('saleInfoManage/getSalePlaceInfo', {
        pn: this.currentPage,
        size: this.pageSize,
        keyword,
      });
    },
    // 新增销售地点
    handleAddSalePlace(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.addFormVisible = false;
          this.$store.dispatch('saleInfoManage/addSalePlace', {
            saleName: this.addForm.saleName,
            salePhone: this.addForm.salePhone,
            address: this.addForm.address,
            longitude: this.addForm.longitude,
            latitude: this.addForm.latitude,
            size: this.pageSize,
          });
        } else {
          this.$message({
            message: '请检查输入的内容是否合规',
            type: 'warning',
          });
        }
      });
    },
    // 删除销售地点
    handleDeleteSalePlace(saleId, saleName) {
      this.$confirm(`确定要删除“${saleName}”的相关信息吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          this.$store.dispatch('saleInfoManage/deleteSalePlace', {
            saleId,
            pn: this.currentPage,
            size: this.pageSize,
            keyword: this.keyword,
          });
        })
        .catch(() => {
          this.$message({
            type: 'info',
            message: '已取消删除',
          });
        });
    },
    // 控制修改销售地点信息的表单弹出
    handleModifyFormVisible(saleInfo) {
      this.modifyForm = {
        saleId: saleInfo.saleId,
        saleName: saleInfo.saleName,
        salePhone: saleInfo.salePhone,
        address: saleInfo.address || '',
        longitude: this.toCoordinate(saleInfo.longitude),
        latitude: this.toCoordinate(saleInfo.latitude),
      };
      this.modifyFormVisible = true;
    },
    // 修改销售地点信息
    handleModifySalePlace(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.modifyFormVisible = false;
          this.$store.dispatch('saleInfoManage/modifySalePlaceInfo', {
            saleId: this.modifyForm.saleId,
            saleName: this.modifyForm.saleName,
            salePhone: this.modifyForm.salePhone,
            address: this.modifyForm.address,
            longitude: this.modifyForm.longitude,
            latitude: this.modifyForm.latitude,
            pn: this.currentPage,
            size: this.pageSize,
            keyword: this.keyword,
          });
        } else {
          this.$message({
            message: '请检查输入的内容是否合规',
            type: 'warning',
          });
        }
      });
    },
    // 每次关闭表单的时候清除验证器和输入框内容
    handleAddClose() {
      this.addForm = emptySaleForm();
      this.addStatus = 0;
      this.$nextTick(() => this.$refs.addForm && this.$refs.addForm.clearValidate());
    },
    handleModifyClose() {
      this.$refs.modifyForm.resetFields();
    },
    toCoordinate(value) {
      if (value === '' || value === null || value === undefined) {
        return null;
      }
      const coordinate = Number(value);
      return Number.isFinite(coordinate) ? coordinate : null;
    },
    hasCoordinate(saleInfo) {
      return (
        this.toCoordinate(saleInfo.longitude) !== null &&
        this.toCoordinate(saleInfo.latitude) !== null
      );
    },
    formatCoordinate(saleInfo) {
      const longitude = this.toCoordinate(saleInfo.longitude);
      const latitude = this.toCoordinate(saleInfo.latitude);
      return `${longitude.toFixed(6)}, ${latitude.toFixed(6)}`;
    },
    // ===== 地图相关 =====
    // 切换列表/地图视图
    handleVisualizationChange(val) {
      if (!val) {
        return;
      }
      // 首次打开时懒加载地图；已加载则 resize 修正尺寸并刷新标记点
      if (!this.map) {
        this.$nextTick(() => this.loadMap());
      } else {
        this.$nextTick(() => {
          this.map.resize();
          this.refreshMap();
        });
      }
    },
    // 点击「新增地点」按钮，进入等待地图点击状态
    handleAdd() {
      this.addStatus = 1;
      this.$message({
        message: '请点击地图上的位置',
        type: 'warning',
      });
    },
    // 地图点击后，经纬度逆地理编码为地址，打开新增弹窗
    creatLocation(lng, lat) {
      const webKey = import.meta.env.VITE_AMAP_WEB_KEY;
      const url = `https://restapi.amap.com/v3/geocode/regeo?key=${webKey}&output=json&location=${lng},${lat}`;
      axios
        .get(url)
        .then((res) => {
          const formatted =
            res &&
            res.data &&
            res.data.regeocode &&
            res.data.regeocode.formatted_address;
          this.addForm.longitude = lng;
          this.addForm.latitude = lat;
          this.addForm.address = formatted || '';
          this.addStatus = 0;
          this.$message({
            showClose: true,
            message: '位置选择成功，请输入详细信息',
            type: 'success',
          });
          this.addFormVisible = true;
        })
        .catch((error) => {
          // 逆地理编码失败时仍允许新增，只是地址留空
          console.log(error);
          this.addForm.longitude = lng;
          this.addForm.latitude = lat;
          this.addForm.address = '';
          this.addStatus = 0;
          this.addFormVisible = true;
        });
    },
    // 清除旧标记点，按全量销售地点重新渲染
    refreshMap() {
      if (!this.map || !this.amap) {
        return;
      }
      if (this.markers.length > 0) {
        this.map.remove(this.markers);
      }
      this.markers = [];
      const list = this.mapData.list || [];
      list.forEach((element) => {
        if (!this.hasCoordinate(element)) {
          return; // 跳过无坐标的销售地点
        }
        const marker = new this.amap.Marker({
          title: element.saleName,
          position: [element.longitude, element.latitude],
        });
        // 点击标记点 -> 打开修改弹窗
        marker.on('click', () => {
          this.handleModifyFormVisible(element);
        });
        this.markers.push(marker);
        this.map.add(marker);
      });
    },
    // 加载高德地图
    loadMap() {
      const jsKey = this.amapJsKey;
      if (!jsKey) {
        return;
      }
      // JS API 2.0 安全密钥配置，必须在 load 之前设置
      window._AMapSecurityConfig = {
        securityJsCode: import.meta.env.VITE_AMAP_SECURITY_CODE || '',
      };
      AMapLoader.load({
        key: jsKey,
        version: '2.0',
        plugins: [],
      })
        .then((AMap) => {
          this.amap = AMap;
          this.map = new AMap.Map('mapContainer', {
            mapStyle: 'amap://styles/whitesmoke',
            zoom: 11,
            center: [104.06707, 30.660842], // 成都
          });
          // 点击地图：若处于新增等待状态，取经纬度逆地理编码
          this.map.on('click', (e) => {
            if (this.addStatus === 1) {
              this.creatLocation(e.lnglat.getLng(), e.lnglat.getLat());
            }
          });
          this.refreshMap();
        })
        .catch((e) => {
          console.log('高德地图加载失败', e);
        });
    },
  },
  watch: {
    // 全量数据变化（增删改后）时，地图视图自动刷新标记点
    mapData: {
      handler() {
        if (this.visualization && this.map) {
          this.refreshMap();
        }
      },
      deep: true,
    },
  },
  mounted() {
    this.getSalePlaceInfo(); // 首次渲染列表
    this.getAllSalePlaceInfo(); // 预加载全量数据，地图打开即可标点
  },
  computed: {
    // 后端返回的数据
    ...mapGetters({
      tableData: 'salePlaceInfo',
      mapData: 'saleAllPlaceInfo',
    }),
    // 用户输入的关键字
    keyword: {
      get() {
        return this.keywordDefault;
      },
      set(val) {
        this.keywordDefault = val;
      },
    },
    amapJsKey() {
      return import.meta.env.VITE_AMAP_JS_KEY || '';
    },
  },
};
</script>

<style lang="less" scoped>
@import "../../style/infoManage.less";
.coordinate-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 12px;
  color: #167f77;
  background: #e9f8f5;
  font-family: Consolas, monospace;
  font-size: 12px;
  white-space: nowrap;
}
.coordinate-empty {
  color: #a5afae;
}
.coordinate-hint {
  margin-left: 10px;
  color: #8c939d;
  font-size: 12px;
}
.map-switch {
  margin: 12px 0 16px;
}
.map-panel {
  margin-top: 8px;
}
.map-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.map-hint {
  color: #e6a23c;
  font-size: 13px;
}
.map-container {
  width: 100%;
  height: 700px;
  border: 1px solid #e5e9ef;
  border-radius: 8px;
  overflow: hidden;
}
.map-placeholder {
  padding: 24px;
  color: #909399;
  text-align: center;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
}
</style>
