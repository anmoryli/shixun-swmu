<template>
    <div class="LoginBackground">
      <div class="LoginForm" >
        <div class="ios-login-brand">
          <img src="../assets/HomeLogo.png" alt="慧医数字医疗应用系统" />
          <p>安全、清晰地管理医疗基础数据</p>
        </div>
        <div class="big-contain" >
          <el-form :model="loginForm" ref="loginForm" status-icon>
            <div class="bform">
              <el-form-item prop="username" :rules="loginRules.usernameRules" class="NameNotNull">
                <div class="UserName">
                    <input
                            class="Input"
                            type="text"
                            v-model="loginForm.username"
                            autocomplete="off"
                            placeholder="用户名"
                            aria-label="用户名"
                    />
                </div>
              </el-form-item>
              <el-form-item prop="password" :rules="loginRules.passwordRules" class="PasswordNotNull">
                <div class="PassWord">
                    <input
                            class="Input"
                            type="password"
                            v-model="loginForm.password"
                            autocomplete="off"
                            placeholder="密码"
                            aria-label="密码"
                            @keydown.enter="handleLogin('loginForm')"
                    />
                </div>
              </el-form-item>
              <el-form-item>
                  <button
                    type="button"
                    class="LoginBtn"
                    :class="{ 'is-loading': loggingIn }"
                    :disabled="loggingIn"
                    :aria-label="loggingIn ? '正在登录' : '登录'"
                    @click="handleLogin('loginForm')"
                  >
                    <span class="ios-only">{{ loggingIn ? "正在登录" : "登录系统" }}</span>
                  </button>
              </el-form-item>
            </div>
          </el-form>
        </div>
      </div>
    </div>
</template>
<script>
import { loginRules } from '../utils/validator';

export default {
  data() {
    return {
      loginForm: {
        username: '',
        password: '',
      },
      loginRules,
      loggingIn: false,
    };
  },
  methods: {
    // 登录事件
    handleLogin(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          if (this.loggingIn) {
            return;
          }
          this.loggingIn = true;
          this.$store
            .dispatch('app/login', this.loginForm)
            .then(() => this.$store.dispatch('app/setMenuList'))
            .then(() => {
              this.$router.replace('/');
            })
            .catch((error) => {
              const isNetworkError = Boolean(
                error && (error.isAxiosError || error.request) && !error.response
              );
              // Cookie authentication has no client-readable token. Always
              // clear the provisional session when menu loading/login fails.
              if (this.$store.state.app && !this.$store.state.app.routesLoaded) {
                this.$store.dispatch('app/logout');
              }
              this.$message.error(
                isNetworkError
                  ? '无法连接服务器，请检查网络或后端服务'
                  : error.message || '账号或密码错误'
              );
            })
            .finally(() => {
              this.loggingIn = false;
            });
        } else {
          this.$notify.error({
            title: ' 错误',
            message: ' 请输入正确的用户名密码',
          });

        }
      });
    }
  },
};
</script>
<style scoped>
  .ios-login-brand,
  .ios-only {
    display: none;
  }
  .LoginBackground{
    background: url("../assets/LoginBackground.jpg") no-repeat;
    background-size: cover;
    width: 100%;
    height: 100%;
    overflow: hidden;

  }
  .LoginForm{
    background: url("../assets/Login.png") no-repeat;
    background-size: 800px,599px;
    width: 1000px;
    height: 800px;
    margin-left: 555px;
    margin-top: 100px;
    overflow: hidden;
  }
  .UserName{
      overflow: hidden;
      border:2px solid #2abeb2;
      border-radius: 20px;
      margin-top: 270px;
      margin-left: -60px;
      height: 70px;
      width: 530px;
  }
  .PassWord{
      overflow: hidden;
      border:2px solid #2abeb2;
      border-radius: 20px;
      margin-top: 20px;
      margin-left: -60px;
      height: 70px;
      width: 530px;
  }
  .Input{
      overflow: hidden;
      margin-left: 20px;
      height: 80px;
      font-size: 35px;
      color: #2abeb2;

  }
  .Logo{
      overflow: hidden;
      margin-left: 15px;
      margin-bottom: 5px;
  }
  .LoginBtn{
      overflow: hidden;
      background: url("../assets/LoginButton.png");
      background-size: 100%;
      width: 220px;
      height: 65px;
      margin-left: 290px;
      margin-top: 10px;
  }
  .LoginBtn:disabled {
      cursor: wait;
      opacity: 0.65;
  }
  .LoginBtn.is-loading {
      filter: grayscale(30%);
  }
  .NameNotNull{
      margin-left: 200px;

  }
  .PasswordNotNull{
      margin-left: 200px;
  }
  input::-webkit-input-placeholder {
      color: #2abeb2;
  }
  input::-ms-input-placeholder {
      color: #2abeb2;
  }
  </style>
