<template>
  <div>
    <Exception type="500">
      <template #desc>
        抱歉，服务器出错了, 或检查您的网络。
      </template>
    </Exception>
  </div>
</template>
<script>
import router from "@/router";
import Common from "@/common/Common";

export default {
  created() {
    localStorage.removeItem(Common.token);
    localStorage.removeItem(Common.menu)
  },
  methods: {
    handlerGoSignIn() {
      router.push('/auth/signin');
    }
  }
}
</script>
