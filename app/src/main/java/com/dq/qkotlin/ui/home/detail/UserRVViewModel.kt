package com.dq.qkotlin.ui.home.detail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dq.qkotlin.bean.UserBaseBean
import com.dq.qkotlin.net.*
import com.dq.qkotlin.tool.NET_ERROR
import com.dq.qkotlin.tool.checkResponseCodeAndThrow
import com.dq.qkotlin.tool.requestCommon
import com.dq.qkotlin.ui.base.BaseRVPagerViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class UserRVViewModel: BaseRVPagerViewModel<UserBaseBean>() {

    //按MVVM设计原则，请求网络应该放到更下一层的"仓库类"里，但是我感觉如果你只做网络不做本地取数据，没必要
    //请求用户列表接口
    fun requestUserList(params : HashMap<String,String> , loadmore : Boolean){

        //调用"万能列表接口封装"
        super.requestList(params, loadmore){

            //用kotlin高阶函数，传入本Activity的"请求用户列表接口的代码块" 就是这3行代码
            var apiService : UserApiService = RetrofitInstance.instance.create(UserApiService::class.java)
            val response: ResponsePageEntity<UserBaseBean> = apiService.userList(params)
            response
        }
    }

    /******* 关注 ********/

    //请求关注接口（MVC的调用方式）
    fun requestFollowByMVC(params : HashMap<String,String>, responseCallback: NetworkResponseCallback<ResponseEntity<Object>>){

        val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            //访问网络异常的回调用, 这种方法可以省去try catch, 但不适用于async启动的协程
            //这里是主线程；
            responseCallback?.onResponse(null, NET_ERROR)
        }

        /*MainScope()是一个绑定到当前viewModel的作用域  当ViewModel被清除时会自动取消该作用域，所以不用担心内存泄漏为问题*/
        viewModelScope.launch(coroutineExceptionHandler) {

            var apiService : UserApiService = RetrofitInstance.instance.create(UserApiService::class.java)
            //suspend 是这一步
            val response: ResponseEntity<Object> = apiService.userFollow(params)
            //如果网络访问异常，代码会直接进入CoroutineExceptionHandler，不会走这里了
            //这里是主线程
            responseCallback?.onResponse(response, null)
        }
    }

    //关注接口请求状态：key是userid（int） value是：关注状态，0未关注，1已经关注，这里用int以免以后出现 互相关注3 。如果是点赞，那用bool就行了
    //因为不是每次进入Activity都会调用这个接口，所以用lazy节省内存
    var followRequestStateMap: MutableLiveData<Pair<Int ,Int>>? = null

    //关注接口请求 - 错误message，要显示给用户看。你也可以统一用一个toastMessage，但是用统一message的弊端是无法针对某个接口的错误做特殊UI定制
    var followRequestErrorMessage: String? = null

    //请求关注接口（MVVM的调用方式）
    fun requestFollowByMVVM(to_userid: Int, currentValue: Int, idealValue: Int, params : HashMap<String,String>) {

        if (followRequestStateMap == null){
            followRequestStateMap = MutableLiveData<Pair<Int ,Int>>()
        }

        //请求服务器之前，我就发出广播
        val loadingMap = Pair(to_userid, idealValue)
        followRequestStateMap!!.value = loadingMap

        val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            //访问网络异常的回调用, 这种方法可以省去try catch, 但不适用于async启动的协程
            //这里是主线程；
            followRequestErrorMessage = NET_ERROR
            //给Activity回调网络请求失败
            val errorMap = Pair(to_userid, currentValue)
            followRequestStateMap!!.value = errorMap
        }

        /*MainScope()是一个绑定到当前viewModel的作用域  当ViewModel被清除时会自动取消该作用域，所以不用担心内存泄漏为问题*/
        viewModelScope.launch(coroutineExceptionHandler) {

            var apiService : UserApiService = RetrofitInstance.instance.create(UserApiService::class.java)
            //suspend 是这一步
            val responseEntry: ResponseEntity<Object> = apiService.userFollow(params)
            //如果网络访问异常，代码会直接进入CoroutineExceptionHandler，不会走这里了
            //这里是主线程
            if (responseEntry == null){
                //进入这里，说明是服务器崩溃，errorMessage是我们本地自定义的
                followRequestErrorMessage = NET_ERROR
                //给Activity回调网络请求失败
                val errorMap =  Pair(to_userid, currentValue)
                followRequestStateMap!!.value = errorMap

                return@launch

            } else if (!responseEntry.isSuccess){
                //进入这里，说明是服务器验证参数错误，message是服务器返回的
                followRequestErrorMessage = responseEntry.message
                //给Activity回调code失败
                val errorMap = Pair(to_userid , currentValue)
                followRequestStateMap!!.value = errorMap

                return@launch
            }

            //请求成功了，因为在请求前已经提前发出成功通知了，真的成功就不用发了
        }
    }


    /******* 删除 ********/

    //请求删除接口（MVC的调用方式）
    fun requestDeleteByMVC(params : HashMap<String,String>, responseCallback: NetworkResponseCallback<ResponseEntity<Object>>){

        val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            //访问网络异常的回调用, 这种方法可以省去try catch, 但不适用于async启动的协程
            //这里是主线程；

            //如果404，代码会进入这里
            responseCallback?.onResponse(null, NET_ERROR)
        }

        /*MainScope()是一个绑定到当前viewModel的作用域  当ViewModel被清除时会自动取消该作用域，所以不用担心内存泄漏为问题*/
        viewModelScope.launch(coroutineExceptionHandler) {

            var apiService : UserApiService = RetrofitInstance.instance.create(UserApiService::class.java)
            //suspend 是这一步
            val response: ResponseEntity<Object> = apiService.userDelete(params)
            //如果网络访问异常，代码会直接进入CoroutineExceptionHandler，不会走这里了
            //这里是主线程
            responseCallback?.onResponse(response, null)
        }
    }


    //删除接口请求状态：key是userid（int） value是：请求中、请求成功、请求失败。
    //因为不是每次进入Activity都会调用这个接口，所以用lazy节省内存
    var deleteRequestStatePair: MutableLiveData<Pair<Int, LoadState>>? = null

    //删除接口请求 - 错误message，要显示给用户看。你也可以统一用一个toastMessage，但是用统一message的弊端是无法针对某个接口的错误做特殊UI定制
    var deleteRequestErrorMessage: String? = null

    //请求删除接口（MVVM的调用方式）
    fun requestDeleteByMVVM(to_userid: Int, params : HashMap<String,String>){

        if (deleteRequestStatePair == null){
            deleteRequestStatePair = MutableLiveData<Pair<Int , LoadState>>()
        }

        val loadingPair = Pair(to_userid, LoadState.Loading)
        deleteRequestStatePair!!.value = loadingPair

        val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            //访问网络异常的回调用, 这种方法可以省去try catch, 但不适用于async启动的协程
            //这里是主线程；
            deleteRequestErrorMessage = NET_ERROR
            //给Activity回调网络请求失败
            val errorPair = Pair(to_userid, LoadState.NetworkFail)
            deleteRequestStatePair!!.value = errorPair
        }

        /*MainScope()是一个绑定到当前viewModel的作用域  当ViewModel被清除时会自动取消该作用域，所以不用担心内存泄漏为问题*/
        viewModelScope.launch(coroutineExceptionHandler) {

            var apiService : UserApiService = RetrofitInstance.instance.create(UserApiService::class.java)
            //suspend 是这一步
            val responseEntry: ResponseEntity<Object> = apiService.userDelete(params)
            //如果网络访问异常，代码会直接进入CoroutineExceptionHandler，不会走这里了
            //这里是主线程
            if (responseEntry == null){
                //进入这里，说明是服务器崩溃，errorMessage是我们本地自定义的
                deleteRequestErrorMessage = NET_ERROR
                //给Activity回调网络请求失败
                val errorPair = Pair(to_userid, LoadState.NetworkFail)
                deleteRequestStatePair!!.value = errorPair

                return@launch

            } else if (!responseEntry.isSuccess){
                //进入这里，说明是服务器验证参数错误，message是服务器返回的
                deleteRequestErrorMessage = responseEntry.message
                //给Activity回调code失败
                val errorPair = Pair(to_userid , LoadState.NetworkFail)
                deleteRequestStatePair!!.value = errorPair

                return@launch
            }

            //给Activity回调成功
            val successPair = Pair(to_userid , LoadState.SuccessNoMore)
            deleteRequestStatePair!!.value = successPair
        }
    }


    //用BaseViewModel里的requireCommon的Demo展示
    fun requestListByMVC(params : HashMap<String,String>, successCallback: NetworkSuccessCallback<PageData<UserBaseBean>>, failCallback: NetworkFailCallback){

        requestCommon(viewModelScope, {
            var apiService : UserApiService = RetrofitInstance.instance.create(UserApiService::class.java)
            //suspend 是这一步
            val response: ResponsePageEntity<UserBaseBean> = apiService.userList(params)
            //如果网络访问异常，代码会直接进入CoroutineExceptionHandler，不会走这里了
            //这里是主线程

            //检查服务器返回的数据的code是否是success。如果是失败，代码会走到failCallback
            checkResponseCodeAndThrow(response)

            //给Activity回调成功
            successCallback?.onResponseSuccess(response.data)
            }

            ,failCallback
        )
    }

}