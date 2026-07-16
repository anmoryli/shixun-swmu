<template>
  <el-container>
    <el-header height="76px">
      <h2>销售地点地图</h2>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>销售地点地图</el-breadcrumb-item>
      </el-breadcrumb>
    </el-header>
    <el-main>
      <div class="map-toolbar">
        <el-button type="primary" v-if="$can('sale:write')" @click="handleAdd">
          新增地点
        </el-button>
        <span v-if="addStatus === 1" class="map-hint">
          <i class="el-icon-location-outline"></i>
          请在地图上点击要新增的位置
        </span>
        <span class="map-stat">共 {{ markerCount }} 个销售地点</span>
      </div>
      <div id="mapContainer" class="map-container"></div>
      <div v-if="mapLoadError" class="map-placeholder" style="color:#f56c6c;border-color:#fbc4c4;">{{ mapLoadError }}</div>
      <div v-if="!amapJsKey" class="map-placeholder">
        未配置高德地图 Key，请在 .env 设置 VITE_AMAP_JS_KEY 后重新构建
      </div>

      <!-- 新增弹窗 -->
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
          <el-form-item label="药店电话" prop="salePhone" :rules="rules.phoneRules">
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
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="addFormVisible = false">取 消</el-button>
            <el-button type="primary" @click="handleAddSalePlace('addForm')">
              确 定
            </el-button>
          </div>
        </template>
      </el-dialog>

      <!-- 修改弹窗 -->
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
            <el-input v-model="modifyForm.saleId" autocomplete="off" disabled></el-input>
          </el-form-item>
          <el-form-item label="药店名称" prop="saleName" :rules="rules.nameRules">
            <el-input
              v-model.trim="modifyForm.saleName"
              autocomplete="off"
              required
            ></el-input>
          </el-form-item>
          <el-form-item label="药店电话" prop="salePhone" :rules="rules.phoneRules">
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
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="modifyFormVisible = false">取 消</el-button>
            <el-button type="primary" @click="handleModifySalePlace('modifyForm')">
              确 定
            </el-button>
          </div>
        </template>
      </el-dialog>
    </el-main>
  </el-container>
</template>

<script>
import { mapGetters } from 'vuex';
import rules from '../../utils/validator';
import AMapLoader from '@amap/amap-jsapi-loader';
import service from '../../utils/request';

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
  name: 'SaleMap',
  // 销售地点地图视图:基于高德 JSAPI 2.0,加载失败显示 mapLoadError 提示。
  data() {
    return {
      addFormVisible: false,
      addForm: emptySaleForm(),
      modifyFormVisible: false,
      modifyForm: {
        saleId: '',
        saleName: '',
        salePhone: '',
        address: '',
        longitude: null,
        latitude: null,
      },
      rules,
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
      map: null,
      amap: null,
      markers: [],
      addStatus: 0,
      markerCount: 0,
      mapLoadError: '',
    };
  },
  computed: {
    ...mapGetters({
      mapData: 'saleAllPlaceInfo',
    }),
    amapJsKey() {
      return import.meta.env.VITE_AMAP_JS_KEY || '';
    },
  },
  methods: {
    canWrite() {
      return typeof this.$can === 'function' && this.$can('sale:write');
    },
    getAllSalePlaceInfo() {
      return this.$store.dispatch('saleInfoManage/getAllSalePlaceInfo');
    },
    handleAdd() {
      if (!this.canWrite()) return;
      this.addStatus = 1;
      this.$message({ message: '请点击地图上的位置', type: 'warning' });
    },
    creatLocation(lng, lat) {
      service
        .get('/regeo', { params: { lng, lat } })
        .then((res) => {
          const formatted = res && res.data && res.data.data;
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
          console.warn(error);
          this.addForm.longitude = lng;
          this.addForm.latitude = lat;
          this.addForm.address = '';
          this.addStatus = 0;
          this.addFormVisible = true;
        });
    },
    refreshMap() {
      if (!this.map || !this.amap) {
        return;
      }
      if (this.markers.length > 0) {
        this.map.remove(this.markers);
      }
      this.markers = [];
      const list = this.mapData.list || [];
      let count = 0;
      list.forEach((element) => {
        const lng = this.toCoordinate(element.longitude);
        const lat = this.toCoordinate(element.latitude);
        if (lng === null || lat === null) {
          return;
        }
        const marker = new this.amap.Marker({
          title: element.saleName,
          position: [lng, lat],
        });
        marker.on('click', () => {
          if (this.canWrite()) {
            this.handleModifyFormVisible(element);
          }
        });
        this.markers.push(marker);
        this.map.add(marker);
        count += 1;
      });
      this.markerCount = count;
    },
    // 加载高德 JSAPI 2.0:CSP 需放行 unsafe-eval/unsafe-inline/alicdn,否则底图白屏
    loadMap() {
      const jsKey = this.amapJsKey;
      if (!jsKey) {
        return;
      }
      window._AMapSecurityConfig = {
        securityJsCode: import.meta.env.VITE_AMAP_SECURITY_CODE || '',
      };
      AMapLoader.load({ key: jsKey, version: '2.0', plugins: [] })
        .then((AMap) => {
          this.amap = AMap;
          this.map = new AMap.Map('mapContainer', {
            mapStyle: 'amap://styles/whitesmoke',
            zoom: 11,
            center: [104.06707, 30.660842],
          });
          this.map.on('click', (e) => {
            if (this.addStatus === 1) {
              this.creatLocation(e.lnglat.getLng(), e.lnglat.getLat());
            }
          });
          this.refreshMap();
        })
        .catch((e) => {
          this.mapLoadError = '地图底图加载失败，请检查网络或刷新重试';
          console.warn('高德地图加载失败', e);
        });
    },
    handleAddSalePlace(formName) {
      if (!this.canWrite()) return;
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.addFormVisible = false;
          this.$store.dispatch('saleInfoManage/addSalePlace', {
            saleName: this.addForm.saleName,
            salePhone: this.addForm.salePhone,
            address: this.addForm.address,
            longitude: this.addForm.longitude,
            latitude: this.addForm.latitude,
            size: 5,
          });
        } else {
          this.$message({ message: '请检查输入的内容是否合规', type: 'warning' });
        }
      });
    },
    handleModifyFormVisible(saleInfo) {
      if (!this.canWrite()) return;
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
    handleModifySalePlace(formName) {
      if (!this.canWrite()) return;
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
            pn: 1,
            size: 5,
          });
        } else {
          this.$message({ message: '请检查输入的内容是否合规', type: 'warning' });
        }
      });
    },
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
  },
  watch: {
    mapData: {
      handler() {
        if (this.map) {
          this.refreshMap();
        }
      },
      deep: true,
    },
  },
  mounted() {
    this.getAllSalePlaceInfo().then(() => {
      this.$nextTick(() => this.loadMap());
    });
  },
  beforeUnmount() {
    // 组件卸载时销毁地图实例与标记,避免 AMap 内部事件监听/DOM 残留导致内存泄漏
    if (this.map) {
      try {
        this.map.destroy();
      } catch (e) {
        // 销毁异常忽略,不阻塞卸载
      }
      this.map = null;
    }
    this.markers = [];
    this.amap = null;
  },
};
</script>

<style lang="less" scoped>
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
.map-stat {
  margin-left: auto;
  color: #909399;
  font-size: 13px;
}
.map-container {
  width: 100%;
  height: 700px;
  border: 1px solid #c8d2cc;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(41, 91, 87, 0.07);
}
.map-placeholder {
  padding: 24px;
  color: #909399;
  text-align: center;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
}
.coordinate-hint {
  margin-left: 10px;
  color: #8c939d;
  font-size: 12px;
}
</style>
