package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardActionCircle
import com.example.ui.theme.CardSalesGradEnd
import com.example.ui.theme.CardSalesGradStart
import com.example.ui.theme.MohasebCyanAccent
import com.example.ui.theme.MohasebDarkCyan
import com.example.ui.theme.MohasebPrimaryBlue

@Composable
fun DashboardGrid(
  onSalesListClick: () -> Unit,
  onNewSaleClick: () -> Unit,
  onPurchasesListClick: () -> Unit,
  onNewPurchaseClick: () -> Unit,
  onVouchersClick: () -> Unit,
  onAccountsClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Row 1: Vouchers (قبض/صرف) and Sales (المبيعات)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(modifier = Modifier.weight(1f)) {
        QuickGridCard(
          title = "المبيعات",
          icon = Icons.Default.ShoppingCart,
          showAddButton = true,
          onCardClick = onSalesListClick,
          onAddClick = onNewSaleClick,
          testTag = "card_sales"
        )
      }

      Box(modifier = Modifier.weight(1f)) {
        QuickGridCard(
          title = "قبض/\nصرف",
          icon = Icons.Default.Receipt,
          showAddButton = false,
          onCardClick = onVouchersClick,
          onAddClick = null,
          testTag = "card_vouchers"
        )
      }
    }

    // Row 2: Purchases (المشتريات) and Accounts (الحسابات)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(modifier = Modifier.weight(1f)) {
        QuickGridCard(
          title = "المشتريات",
          icon = Icons.Default.ShoppingCart,
          showAddButton = true,
          onCardClick = onPurchasesListClick,
          onAddClick = onNewPurchaseClick,
          testTag = "card_purchases"
        )
      }

      Box(modifier = Modifier.weight(1f)) {
        QuickGridCard(
          title = "الحسابات",
          icon = Icons.Default.AccountBalance,
          showAddButton = false,
          onCardClick = onAccountsClick,
          onAddClick = null,
          testTag = "card_accounts"
        )
      }
    }
  }
}

@Composable
fun QuickGridCard(
  title: String,
  icon: ImageVector,
  showAddButton: Boolean,
  onCardClick: () -> Unit,
  onAddClick: (() -> Unit)?,
  testTag: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    // Main cyan gradient pill/box
    Surface(
      modifier = Modifier
        .weight(1f)
        .height(76.dp)
        .shadow(4.dp, RoundedCornerShape(14.dp))
        .clip(RoundedCornerShape(14.dp))
        .border(1.5.dp, Color(0xFF80DEEA), RoundedCornerShape(14.dp))
        .clickable { onCardClick() }
        .testTag(testTag),
      color = Color.Transparent
    ) {
      Box(
        modifier = Modifier
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color(0xFF00ACC1),
                Color(0xFF00838F),
                Color(0xFF006064)
              )
            )
          )
          .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = title,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
        }
      }
    }

    // Circled (+) button if applicable
    if (showAddButton && onAddClick != null) {
      Spacer(modifier = Modifier.width(6.dp))
      Box(
        modifier = Modifier
          .size(38.dp)
          .shadow(3.dp, CircleShape)
          .clip(CircleShape)
          .background(Color(0xFF004D40))
          .border(1.dp, Color(0xFF80CBC4), CircleShape)
          .clickable { onAddClick() }
          .testTag(testTag + "_add_button"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "إضافة $title",
          tint = Color.White,
          modifier = Modifier.size(24.dp)
        )
      }
    }
  }
}
