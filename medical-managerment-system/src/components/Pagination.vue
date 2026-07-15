<template>
  <div class="pagination-container">
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="currentPageSize"
      :page-sizes="pageSizes"
      :layout="layout"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script>
export default {
  name: 'Pagination',
  props: {
    total: {
      required: true,
    },
    page: {
      type: Number,
      default: 1,
    },
    layout: {
      type: String,
      default: 'total, sizes, prev, pager, next, jumper',
    },
    pageSize: {
      type: Number,
      default: 10,
    },
    pageSizes: {
      type: Array,
      default: () => [10, 20, 50],
    },
  },
  computed: {
    currentPage: {
      get() {
        return this.page;
      },
      set(val) {
        this.$emit('update:page', val);
      },
    },
    currentPageSize: {
      get() {
        return this.pageSize;
      },
      set(val) {
        this.$emit('update:pageSize', val);
      },
    },
  },
  methods: {
    handleSizeChange(val) {
      this.$emit('currentChange', { page: this.currentPage, limit: val });
    },
    handleCurrentChange(val) {
      this.$emit('currentChange', { page: val, limit: this.pageSize });
    },
  },
};
</script>
