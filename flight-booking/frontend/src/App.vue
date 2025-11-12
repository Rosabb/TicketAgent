<template>
  <!-- 使用Element Plus的栅格布局，设置列间距为20px -->
  <el-row :gutter="20">
    <!-- 左侧列，占据16格宽度（在24格系统中） -->
    <el-col :span="16">
      <!-- 数据表格组件，显示航班预订信息 -->
      <el-table :data="tableData" stripe style="width: 100%">
        <!-- 预订编号列 -->
        <el-table-column prop="bookingNumber" label="#"   />
        <!-- 乘客姓名列 -->
        <el-table-column prop="name" label="Name" />
        <!-- 航班日期列 -->
        <el-table-column prop="date" label="Date" />
        <!-- 出发地列 -->
        <el-table-column prop="from" label="From" />
        <!-- 目的地列 -->
        <el-table-column prop="to" label="To" />
        <!-- 预订状态列，使用作用域插槽自定义显示内容 -->
        <el-table-column prop="bookingStatus" label="Status" >
          <template #default="scope">
            <!-- 根据状态显示不同的图标：确认显示✅，否则显示❌ -->
            {{ scope.row.bookingStatus === "CONFIRMED" ? "✅" : "❌"}}
          </template>
        </el-table-column>
        <!-- 预订舱位等级列 -->
        <el-table-column prop="bookingClass" label="Booking class" />
        <!-- 操作列，固定在右侧，宽度180px -->
        <el-table-column label="Operations" fixed="right" width="180" >
          <template #default="scope">
            <!-- 更改预订按钮 -->
            <el-button size="small"
                       type="primary">
              更改预定
            </el-button>
            <!-- 退订按钮 -->
            <el-button
                size="small"
                type="danger">
              退订
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-col>

    <!-- 右侧列，占据8格宽度，设置背景颜色 -->
    <el-col :span="8" style="background-color: aliceblue;">
      <!-- 时间轴容器，设置固定高度和滚动条 -->
      <div style="height: 500px;overflow: scroll">
        <!-- Element Plus时间轴组件 -->
        <el-timeline style="max-width: 100%">
          <!-- 遍历活动数据，生成时间轴项 -->
          <el-timeline-item
              v-for="(activity, index) in activities"
              :key="index"
              :icon="activity.icon"
              :type="activity.type"
              :color="activity.color"
              :size="activity.size"
              :hollow="activity.hollow"
              :timestamp="activity.timestamp"
          >
            <!-- 显示活动内容 -->
            {{ activity.content }}
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- 聊天输入区域容器 -->
      <div id="container">
        <div id="chat">
          <!-- 消息输入框，绑定msg变量，支持回车发送 -->
          <el-input
              v-model="msg"
              input-style="width: 100%;height:50px"
              :rows="2"
              type="text"
              placeholder="Please input"
              @keydown.enter="sendMsg();"
          />
          <!-- 发送消息按钮 -->
          <el-button  @click="sendMsg()">发送</el-button>
        </div>
      </div>
    </el-col>
  </el-row>

</template>

<script lang="ts">
// 导入所需的Vue功能和库
import { MoreFilled } from '@element-plus/icons-vue'
import {ref, onMounted} from "vue";
import axios from 'axios'// 引入axios用于HTTP请求
import { v4 as uuidv4 } from "uuid";// 引入UUID生成唯一会话ID

// 生成唯一的聊天会话ID
const chatId = uuidv4();

export default {
  setup() {
    // 响应式数据：时间轴活动列表，包含初始欢迎消息
    const activities = ref([
      {
        content: '⭐欢迎来到智能航空票务助手✈！请问有什么可以帮您的?',
        timestamp: new Date().toLocaleDateString() + " " + new Date().toLocaleTimeString(),
        color: '#0bbd87',
      },
    ]);

    // 响应式数据：当前输入的消息
    const msg = ref('');
    // 响应式数据：表格数据
    const tableData = ref([]);
    // 计数器，用于跟踪时间轴项的位置
    let count = 2;
    // EventSource对象，用于服务器推送事件
    let eventSource;

    // 发送消息函数
    const sendMsg = () => {
      // 如果已存在EventSource连接，先关闭
      if (eventSource) {
        eventSource.close();
      }

      // 将用户消息添加到时间轴
      activities.value.push(
          {
            content: `你:${msg.value}`,
            timestamp: new Date().toLocaleDateString() + " " + new Date().toLocaleTimeString(),
            size: 'large',
            type: 'primary',
            icon: MoreFilled,
          },
      );

      // 添加等待回复的占位项
      activities.value.push(
          {
            content: 'waiting...',
            timestamp: new Date().toLocaleDateString() + " " + new Date().toLocaleTimeString(),
            color: '#0bbd87',
          },
      );
      /**
       * SSE（Server-Sent Events）是一种允许服务器向客户端推送事件的技术。它是HTML5规范的一部分，基于HTTP协议，
       * 提供了一种简单的方式来从服务器实时更新客户端。与WebSocket不同，SSE是单向通信（服务器到客户端），而WebSocket是双向通信。
       * SSE适用于需要服务器向客户端发送实时消息的场景，例如新闻推送、股票价格更新、聊天应用等。
       *
       * SSE的特点：
       * 使用简单，客户端通过EventSource API连接服务器，服务器返回一个流（stream）并保持连接，然后可以随时发送事件。
       * 自动重连：如果连接断开，客户端会自动尝试重新连接。
       * 支持自定义事件类型。在Vue组件中，我们使用EventSource来建立SSE连接，监听服务器发送的消息，并更新界面。
       */
      // SSE创建EventSource连接，使用服务器推送事件
      eventSource = new EventSource(`http://localhost:8088/api/assistant/chat?chatId=${chatId}&message=${msg.value}`);
      // 清空输入框
      msg.value='';

      // 监听服务器消息事件
      eventSource.onmessage = (event) => {
        // 如果收到完成标记，关闭连接并刷新预订列表
        if (event.data === '[complete]') {
          count = count + 2;
          eventSource.close();
          getBookings();  // 每次对话完后刷新列表
          return;
        }
        // 将服务器返回的数据追加到当前回复内容中
        activities.value[count].content += event.data;
      };

      // 连接打开时的回调
      eventSource.onopen = (event) => {
        // 清空等待回复的占位内容
        activities.value[count].content = '';
      };
    };

    // 获取预订列表函数
    const getBookings = () => {
      axios.get('http://localhost:8088/api/bookings')
          .then((response) => {
            // 将响应数据赋值给表格数据
            tableData.value = response.data;
          })
          .catch((error) => {
            // 错误处理
            console.error(error);
          });
    };

    // 使用onMounted生命周期钩子，在组件挂载时调用getBookings
    onMounted(() => {
      getBookings();
    });

    // 返回模板中需要使用的数据和方法
    return {
      activities,
      msg,
      tableData,
      sendMsg,
      getBookings,
    };
  },
};
</script>

<style scoped>
/* 全局重置样式 */
* {
  margin: 0;
  padding: 0;
}

/* 聊天区域按钮样式 */
#chat button{
  position: absolute;
  margin-left: -60px;
  margin-top: 19px;
}
</style>